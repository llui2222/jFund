package com.xm.jfund.execution;

import com.xm.jfund.client.trade.TradeServiceRestClient;
import com.xm.jfund.client.trade.exception.TradeServiceException;
import com.xm.jfund.client.trade.model.OrderType;
import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.client.trade.model.SubmitTradeRequest;
import com.xm.jfund.client.trade.model.TimeInForce;
import com.xm.jfund.queuemessages.ExposuresMessage;
import com.xm.jfund.queuemessages.TradeFailureExposuresMessage;
import com.xm.jfund.utils.DataLoader;
import com.xm.jfund.utils.NotificationUtils;
import com.xm.jfund.utils.NotificationUtils.NotificationLevel;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import com.xm.jfund.utils.StrategySymbolTradeFrequencyStatus;
import com.xm.jfund.utils.TradeForExecution;
import com.xm.jfund.zabbixobjects.TradeMessage;
import com.xm.jfund.zabbixobjects.ZabbixMessage;
import jxmUtils.Functionals;
import jxmUtils.StringUtils;
import jxmUtils.TupleModule.Pair;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author nmichael
 */
public final class TradeExecutor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TradeExecutor.class);

    public static TradeExecutor create(
        final BlockingQueue<TradeForExecution> tradeBlockingQueue,
        final Map<Integer, StrategyParameters> strategyParametersMap,
        final BlockingDeque<ExposuresMessage> exposureCollectorQueue,
        final BlockingQueue<ZabbixMessage> zabbixMessageQueue,
        final String zabbixTradeTrapperName,
        final TradeServiceRestClient tradeServiceClient,
        final Set<Integer> disabledStrategies) {

        final boolean timeToDie = false;

        final Map<Integer, Map<String, StrategySymbolTradeFrequencyStatus>> strategyToSymbolFrequencyMap = strategyParametersMap.keySet().stream()
            .map(s -> Pair.create(s, new HashMap<String, StrategySymbolTradeFrequencyStatus>()))
            .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));

        return new TradeExecutor(
            strategyParametersMap,
            tradeServiceClient,
            tradeBlockingQueue,
            exposureCollectorQueue,
            zabbixMessageQueue,
            zabbixTradeTrapperName,
            strategyToSymbolFrequencyMap,
            disabledStrategies,
            timeToDie);
    }

    public void timeToDie() {

        mTimeToDie = true;
    }

    @Override
    public void run() {

        logger.info("Trade Executor has started...");

        try {
            while (!mTimeToDie) {

                TradeForExecution tradeForExecution = null;
                try {
                    tradeForExecution = mTradeBlockingQueue.poll(sPollingDelayInMilliSeconds, TimeUnit.MILLISECONDS);
                }
                catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (tradeForExecution != null && !mDisabledStrategies.contains(tradeForExecution.getStrategyId())) {

                    final int strategyId = tradeForExecution.getStrategyId();
                    final String comment = tradeForExecution.getComment();
                    final StrategyParameters strategyParameters = mStrategyParametersMap.get(strategyId);
                    final String takerName = strategyParameters.getExecutionInfo().getTakerName();
                    final String takerLogin = strategyParameters.getExecutionInfo().getTakerLogin();
                    final String requestedSymbolName = tradeForExecution.getSymbol();
                    final TYPE type = tradeForExecution.getTradeStrategySymbolLimitsExecutionType();
                    final Optional<String> lpSymbolName = Optional.ofNullable(strategyParameters.getSymbolToLpNameConversionMap().get(requestedSymbolName));
                    final String tradeSymbol = lpSymbolName.orElse(requestedSymbolName);
                    final BigDecimal requestedVolume = tradeForExecution.getVolume();

                    logger.info("Calling server with order for strategy: {}, symbol: {}, volume {}, comment {}", strategyId, tradeSymbol, requestedVolume, comment);

                    try {
                        final URI location = executeTrade(tradeSymbol, requestedVolume, takerName, takerLogin, comment, tradeForExecution.getSide());
                        logger.info("Trade successfully submitted for execution. Volume: {}, Symbol: {}, Taker name: {}, Taker login: {},  Status url: {}", requestedVolume, tradeSymbol, takerName, takerLogin, location);

                        mZabbixMessageQueue.add(TradeMessage.create(mZabbixTradeTrapperName, strategyId, requestedSymbolName, requestedVolume.doubleValue()));

                        mStrategyToSymbolFrequencyMap.get(strategyId).merge(
                            requestedSymbolName,
                            StrategySymbolTradeFrequencyStatus.create(strategyId, requestedSymbolName, LocalDateTime.now(ZoneOffset.UTC), 1, type),
                            this::mergeAndEnforceFrequencySafeguard);
                    }
                    catch (final TradeServiceException e) {
                        final int statusCode = e.getStatusCode();
                        if (statusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                            final String message = String.format("Internal error from trade service. Status code: %d", statusCode);
                            logger.warn(message, e);
                            throw new RuntimeException(message, e);
                        }
                        else {
                            mExposureCollectorQueue.add(TradeFailureExposuresMessage.create(comment, strategyId, requestedSymbolName));
                            final String errorMessage = StringUtils.makeErrorMessage(String.format("OpenOrderResponse wasn't successful. Http Status code: %d. The request will be zeroed out from the exposures. Strategy: %d, tradeSymbol: %s, volume: %f, taker: %s, marginAccount: %s, comment: %s", statusCode, strategyId, tradeSymbol, requestedVolume, takerName, takerLogin, comment), e);
                            logger.warn(errorMessage);
                            NotificationUtils.notifyAdmins(NotificationLevel.EMAIL_ONLY, errorMessage);
                        }
                    }
                }
            }
        }
        finally {
            logger.info("Trade Executor has ended.");
        }
    }

    private StrategySymbolTradeFrequencyStatus mergeAndEnforceFrequencySafeguard(
        final StrategySymbolTradeFrequencyStatus currentMinuteStatus,
        final StrategySymbolTradeFrequencyStatus newMinuteStatusEntry) {

        final StrategySymbolTradeFrequencyStatus resultingStatus;

        // Check if Strategy Symbol Threshold Type has changed to initiate a new frequency counter
        if (currentMinuteStatus.getCurrentExecutionStrategySymbolLimitType() == newMinuteStatusEntry.getCurrentExecutionStrategySymbolLimitType()) {
            resultingStatus = currentMinuteStatus.getTimeUpToMinute().isEqual(newMinuteStatusEntry.getTimeUpToMinute())
                ? currentMinuteStatus.withIncrement(1)
                : newMinuteStatusEntry;
        }

        else {
            resultingStatus = newMinuteStatusEntry;
        }

        final int strategyId = resultingStatus.getStrategyId();
        final String symbolName = resultingStatus.getSymbolName();
        final int dangerFrequencyLevel = mStrategyParametersMap.get(strategyId).getSymbolLimits().get(symbolName).get(resultingStatus.getCurrentExecutionStrategySymbolLimitType()).getDangerFrequencyLevel();

        if (resultingStatus.getNumberOfTrades() >= dangerFrequencyLevel) {

            mDisabledStrategies.add(strategyId);
            mZabbixMessageQueue.add(TradeMessage.create(mZabbixTradeTrapperName, strategyId, newMinuteStatusEntry.getSymbolName(), sSentinelValueForStrategyBeingRemoved));
            DataLoader.insertDisabledStrategy(strategyId);
            logger.warn("Disabled trading for strategy {}", strategyId);
        }

        return resultingStatus;
    }

    private URI executeTrade(final String symbol, final BigDecimal requestedVolume, final String takerName, final String takerLogin, final String comment, final Side side) throws TradeServiceException {

        final SubmitTradeRequest request = SubmitTradeRequest.create(comment, requestedVolume.abs(), null, takerName, takerLogin, symbol, side, OrderType.MARKET, TimeInForce.GTC);
        return mTradeServiceRestClient.submitTrade(request);
    }

    private static final int sPollingDelayInSeconds = 2;
    private static final int sPollingDelayInMilliSeconds = sPollingDelayInSeconds * 1000;
    private static final double sSentinelValueForStrategyBeingRemoved = -1;

    private TradeExecutor(
        final Map<Integer, StrategyParameters> strategyParametersMap,
        final TradeServiceRestClient tradeServiceClient,
        final BlockingQueue<TradeForExecution> tradeBlockingQueue,
        final BlockingDeque<ExposuresMessage> exposureCollectorQueue,
        final BlockingQueue<ZabbixMessage> zabbixMessageQueue,
        final String zabbixTradeTrapperName,
        final Map<Integer, Map<String, StrategySymbolTradeFrequencyStatus>> strategyToSymbolFrequencyMap,
        final Set<Integer> disabledStrategies,
        final boolean timeToDie) {

        mStrategyParametersMap = strategyParametersMap;
        mTradeServiceRestClient = tradeServiceClient;
        mTradeBlockingQueue = tradeBlockingQueue;
        mExposureCollectorQueue = exposureCollectorQueue;
        mZabbixMessageQueue = zabbixMessageQueue;
        mZabbixTradeTrapperName = zabbixTradeTrapperName;
        mStrategyToSymbolFrequencyMap = strategyToSymbolFrequencyMap;
        mDisabledStrategies = disabledStrategies;
        mTimeToDie = timeToDie;
    }

    private final Map<Integer, StrategyParameters> mStrategyParametersMap;
    private final TradeServiceRestClient mTradeServiceRestClient;
    private final BlockingQueue<TradeForExecution> mTradeBlockingQueue;
    private final BlockingDeque<ExposuresMessage> mExposureCollectorQueue;
    private final BlockingQueue<ZabbixMessage> mZabbixMessageQueue;
    private final String mZabbixTradeTrapperName;
    private final Map<Integer, Map<String, StrategySymbolTradeFrequencyStatus>> mStrategyToSymbolFrequencyMap;
    private final Set<Integer> mDisabledStrategies;

    private volatile boolean mTimeToDie;
}