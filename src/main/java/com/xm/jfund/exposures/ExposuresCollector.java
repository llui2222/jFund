package com.xm.jfund.exposures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.controllers.DecisionController;
import com.xm.jfund.controllers.OrderVolumeCalculationResult;
import com.xm.jfund.controllers.utilities.Danger;
import com.xm.jfund.controllers.utilities.DangerCollector;
import com.xm.jfund.math.MathUtils;
import com.xm.jfund.queuemessages.ExposuresMessage;
import com.xm.jfund.queuemessages.ExposuresVisitor;
import com.xm.jfund.queuemessages.TradeFailureExposuresMessage;
import com.xm.jfund.queuemessages.TradeInfoWithStrategyExposures;
import com.xm.jfund.trade.TradeObject;
import com.xm.jfund.trade.TradeObjectType;
import com.xm.jfund.utils.CurrentActiveThresholdTypeAuditHandler;
import com.xm.jfund.utils.DecisionUtils;
import com.xm.jfund.utils.LogExposuresMode;
import com.xm.jfund.utils.LogFileName;
import com.xm.jfund.utils.NotificationUtils;
import com.xm.jfund.utils.NotificationUtils.NotificationLevel;
import com.xm.jfund.utils.StrategyExecutionInfo;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.StrategySymbolLimits;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import com.xm.jfund.utils.SubmittedTradeMarker;
import com.xm.jfund.utils.TradeForExecution;
import com.xm.jfund.utils.TradeMappingUtils;
import com.xm.jfund.zabbixobjects.ExposureMessage;
import com.xm.jfund.zabbixobjects.TradeMessage;
import com.xm.jfund.zabbixobjects.ZabbixMessage;
import jAnalystUtils.ExposureBundle;
import jAnalystUtils.SymbolModule;
import jAnalystUtils.SymbolModule.SymbolMetaData;
import jAnalystUtils.SymbolModule.SymbolWithDetails;
import jAnalystUtils.TradeExposure;
import jAnalystUtils.TradeInfo;
import jAnalystUtils.TradeInfo.TradeType;
import jAnalystUtils.currencyConversion.CurrencyConversionPump;
import jAnalystUtils.currencyConversion.CurrencyConversionPump.NoSuchSymbolTickException;
import jAnalystUtils.currencyConversion.TickInfoInternal;
import jxmUtils.CompressionModule.XMDeflater;
import jxmUtils.EmailCredentials.JAnalystEmailCredentials;
import jxmUtils.EmailSender;
import jxmUtils.Functionals;
import jxmUtils.StringUtils;
import jxmUtils.exposureInterchange.CurrencyExposureEntry;
import jxmUtils.exposureInterchange.RiskGroupExposure;
import jxmUtils.exposureInterchange.SymbolExposureComputedValues;
import jxmUtils.exposureInterchange.SymbolExposureEntry;
import mt4jUtils.Mt4ArraySizes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author nmichael
 */
public final class ExposuresCollector implements Runnable, ExposuresVisitor {

    private static final Logger logger = LoggerFactory.getLogger(ExposuresCollector.class);
    private static final Logger sCsvLogger = LoggerFactory.getLogger("csvLogger");
    private static final String sLogFileNameParam = "logFile";

    public static ExposuresCollector create(
        final DecisionController decisionController,
        final Map<Integer, StrategyParameters> strategyParametersMap,
        final BlockingDeque<ExposuresMessage> incomingQueue,
        final CurrencyConversionPump conversionPump,
        final SymbolModule.SymbolCache symbolCache,
        final BlockingQueue<TradeForExecution> tradeOutgoingQueue,
        final TradeInfoWithStrategyExposures sentinelTradeInfoWithStrategyExposures,
        final LogExposuresMode logExposuresMode,
        final BlockingQueue<ZabbixMessage> zabbixMessageQueue,
        final String zabbixExposuresTrapperName,
        final String zabbixRequestTrapperName,
        final Map<Integer, BlockingQueue<byte[]>> strategyToExposureSenderQueueMap,
        final Map<Integer, Set<SymbolMetaData>> strategySymbolsMetaData,
        final Set<Integer> tradeDisabledStrategies,
        final boolean isProduction) {

        final Set<Integer> trackedStrategies = strategyParametersMap.keySet();

        final boolean timeToDie = false;
        final boolean startTradingActivity = false;
        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        final ScheduledExecutorService exposureShipmentScheduler = Executors.newScheduledThreadPool(1);

        final Map<Integer, ExposureBundle> strategyExposureMap = trackedStrategies.stream()
            .collect(Functionals.toHashMap(Function.identity(), s -> {
                final ExposureBundle e = ExposureBundle.create(symbolCache);

                final Set<SymbolMetaData> symbolsToPrefill = strategySymbolsMetaData.get(s);

                final Map<String, SymbolExposureEntry> symbolExposureEntries = e.getSymbolExposures();
                final Map<String, CurrencyExposureEntry> currencyExposureEntries = e.getCurrencyExposures();

                symbolsToPrefill.forEach(symbolMetaData -> {
                    final String symbolName = symbolMetaData.getSymbolName();
                    symbolExposureEntries.put(symbolName, SymbolExposureEntry.create(symbolName, symbolMetaData.getBaseAsset()));

                    final String baseAsset = symbolMetaData.getBaseAsset();
                    final String quoteAsset = symbolMetaData.getQuoteAsset();
                    currencyExposureEntries.put(baseAsset, CurrencyExposureEntry.create(baseAsset, baseAsset));
                    currencyExposureEntries.put(quoteAsset, CurrencyExposureEntry.create(quoteAsset, quoteAsset));
                });

                return e;
            }));

        final Gson googleJson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        final boolean fxQuoteSymbolErrorOccurred = false;
        final XMDeflater xmDeflater = XMDeflater.create();

        final Map<String, Double> symbolConversionFactors = new HashMap<>();
        final Map<String, Double> currencyConversionFactors = new HashMap<>();

        final Map<Integer, Integer> strategyAntiCoverageIdGenerator = trackedStrategies.stream()
            .collect(Functionals.toHashMap(Function.identity(), s -> 1));

        final Map<Integer, Map<String, Map<String, SubmittedTradeMarker>>> strategySubmittedTrades = new HashMap<>();
        for (final Integer strategyId : strategyParametersMap.keySet()) {
            strategySubmittedTrades.put(strategyId, new HashMap<>());
            for (final String symbol : strategyParametersMap.get(strategyId).getSymbolLimits().keySet()) {
                strategySubmittedTrades.get(strategyId).put(symbol, new HashMap<>());
            }
        }

        return new ExposuresCollector(
            decisionController,
            strategyParametersMap,
            incomingQueue,
            conversionPump,
            tradeOutgoingQueue,
            symbolCache,
            sentinelTradeInfoWithStrategyExposures,
            logExposuresMode,
            timeToDie,
            startTradingActivity,
            executorService,
            exposureShipmentScheduler,
            strategyExposureMap,
            googleJson,
            fxQuoteSymbolErrorOccurred,
            xmDeflater,
            symbolConversionFactors,
            currencyConversionFactors,
            zabbixMessageQueue,
            zabbixExposuresTrapperName,
            zabbixRequestTrapperName,
            strategyToExposureSenderQueueMap,
            strategyAntiCoverageIdGenerator,
            strategySubmittedTrades,
            tradeDisabledStrategies,
            initialiseSymbolStrategyLimitCurrentState(strategyParametersMap),
            isProduction);
    }

    @Override
    public void visit(final TradeInfoWithStrategyExposures ti) {

        if (ti != mSentinelTradeInfoWithStrategyExposures) {
            accumulateExposures(ti);
        }
        else {
            mStartTradingActivity = true; // We have processed all existing open trades.  We can now start trading activity.

            mStrategyParametersMap.keySet().forEach(s ->
                {
                    MDC.put(sLogFileNameParam, LogFileName.ACTIVITY.forStrategy(s));
                    sCsvLogger.info(",,,,,,,,,,,,,,,Finished setting up -- Starting trading activity -- Unordered block ends here");
                }
            );
        }
    }

    @Override
    public void visit(final TradeFailureExposuresMessage tf) {
        final String comment = tf.getFailedTradeRequestComment();
        final int strategyId = tf.getStrategyId();
        final String symbol = tf.getSymbol();

        final Map<String, SubmittedTradeMarker> submittedTradeRequests = mStrategySubmittedTrades.get(strategyId).get(symbol);
        final ExposureBundle exposureBundle = mStrategyExposureMap.get(strategyId);

        removeTradeRequest(comment, submittedTradeRequests, exposureBundle);
    }

    private static final int sPollingTimeForIncomingMessages = 1000;

    @Override
    public void run() {

        logger.info("Exposures Collector has started...");
        try {
            populateConversionFactors();

            scheduleNextExposureShipment();

            mStrategyParametersMap.keySet().forEach(s ->
            {
                MDC.put(sLogFileNameParam, LogFileName.ACTIVITY.forStrategy(s));
                sCsvLogger.info(",,,,,,,,,,,,,,,Setting up -- Unordered block starts here.");
            });

            while (!mTimeToDie) {

                ExposuresMessage src = null;

                try {
                    src = mIncomingQueue.pollFirst(sPollingTimeForIncomingMessages, TimeUnit.MILLISECONDS);
                }
                catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                if (src != null) {

                    src.accept(this);
                }

                if (mTimeToSendMyWorkOnwards) {
                    populateConversionFactors();

                    final LocalDateTime currentTime = LocalDateTime.now();

                    sendExposuresToTheEther(currentTime);

                    scheduleNextExposureShipment();
                }
            }
        }
        finally {
            cleanUpForExit();
            logger.info("Exposures Collector has ended.");
        }
    }

    private void cleanUpForExit() {

        mExposureShipmentScheduler.shutdown();
        mExecutorService.shutdown();
    }

    public void timeToDie() {
        mTimeToDie = true;
    }

    private static boolean hasTradeImpact(final SymbolExposureComputedValues oldValues, final SymbolExposureComputedValues newValues) {

        if (oldValues == null && Double.compare(newValues.mNetTrades, 0) == 0 && Double.compare(newValues.mNetCoverage, 0) == 0) { //Irrelevant close trade is the first message to come.
            return false;
        }
        else if (oldValues == null) { //First message for that symbol, it had an impact.
            return true;
        }
        else { //Not the first message to reach this thread, calculate normally.
            return (Double.compare(oldValues.mNetTrades, newValues.mNetTrades) != 0) || (Double.compare(oldValues.mNetCoverage, newValues.mNetCoverage) != 0);
        }
    }

    private static void removeTradeRequest(
        final String tradeComment,
        final Map<String, SubmittedTradeMarker> submittedTradeMarkers,
        final ExposureBundle exposureBundle) {

        final SubmittedTradeMarker rm = submittedTradeMarkers.remove(tradeComment);

        if (rm != null) {

            final TradeInfo submittedTradeInfo = rm.getTradeInfo();
            final TradeInfo fakeClosingTradeInfo = TradeInfo.create(submittedTradeInfo.getTradeExp(), submittedTradeInfo.getLogin(), submittedTradeInfo.getSymbol(), submittedTradeInfo.getServerId(), TradeType.CloseTrade, submittedTradeInfo.getGroups());

            exposureBundle.accumulate(fakeClosingTradeInfo, false, true);
        }
    }

    private static void accumulateExposures(final ExposureBundle exposureBundle, final TradeInfo tradeInfo, final boolean isAntiCoverageTrade) {

        exposureBundle.accumulate(tradeInfo, false, isAntiCoverageTrade);
    }

    private static void adjustExposures(
        final ExposureBundle exposureBundle,
        final TradeInfo tradeInfo,
        final String tradeComment,
        final Map<String, SubmittedTradeMarker> submittedTradeMarkers,
        final boolean isClientTrade) {

        if (isClientTrade) {
            accumulateExposures(exposureBundle, tradeInfo, false);
        }
        else {
            /* If we had previously accumulated it with a simulated price, we first zero it out by accumulating a fake closing trade of the same exposure
               There will be no submitted marker found for existing open trades on startup, or trades closing when we do the occasional close-by to accumulate them into one.*/

            removeTradeRequest(tradeComment, submittedTradeMarkers, exposureBundle);

            accumulateExposures(exposureBundle, tradeInfo, true);
        }
    }

    private static TradeInfo accumulateAntiCoverageTrade(
        final ExposureBundle exposureBundle,
        final SymbolWithDetails symbolWithDetails,
        final double volume,
        final double openPrice,
        final long tradingAccount) {

        final boolean isBuy = volume > 0.0;
        final double absoluteVolume = Math.abs(volume);

        final String symbolName = symbolWithDetails.getSymbolName();

        final TradeInfo submittedTradeInfo = TradeInfo.create(
            TradeExposure.createTradeExposure(
                isBuy,
                absoluteVolume,
                1, //always 1
                openPrice,
                symbolWithDetails.getSymbolDetails().getCcyConvFactor(),
                symbolWithDetails.getSymbol().getCalculationSettings().getContractSize(),
                symbolName,
                0L),
            tradingAccount,
            symbolName,
            -1,//we use fix to execute our trades so we don't care about the server id anymore, also it's not used here
            TradeType.OpenTrade,
            new ArrayList<>());

        accumulateExposures(exposureBundle, submittedTradeInfo, true);

        return submittedTradeInfo;
    }

    private static double getSimulatedOpenPrice(final ExposureBundle exposureBundle, final String symbolName, final double volume) {

        final SymbolExposureEntry symbolExposureEntry = exposureBundle.getSymbolExposures().get(symbolName);

        return volume > 0.0
            ? symbolExposureEntry.getAsk()
            : symbolExposureEntry.getBid();
    }

    private void accumulateExposures(final TradeInfoWithStrategyExposures ti) {

        ti.getTradeExposureOnStrategies().forEach(tradeExposureOnStrategy -> {

            final int strategyId = tradeExposureOnStrategy.getStrategyId();
            final StrategyParameters strategyParameters = mStrategyParametersMap.get(strategyId);
            final boolean isClientTrade = tradeExposureOnStrategy.isClientRiskGroup();
            final StrategyExecutionInfo strategyExecutionInfo = strategyParameters.getExecutionInfo();
            final double strategyWeight = strategyExecutionInfo.getStrategyWeight();
            final double indicatorThreshold = strategyExecutionInfo.getIndicatorThreshold();

            final ExposureBundle exposureBundle = mStrategyExposureMap.get(strategyId);
            final double commonGroupWeight = strategyParameters.getCommonClientGroupWeight();

            final TradeObject originalTrade = ti.getTrade();
            final String destSymbol = getParentSymbol(originalTrade.getSymbol(), mSymbolCache);
            final SymbolWithDetails destinationSymbolWithDetails = mSymbolCache.get(destSymbol);
            final double destSymbolContractSize = destinationSymbolWithDetails.getSymbol().getCalculationSettings().getContractSize();

            final long account = originalTrade.getAccount();

            final TradeInfo weightedTradeInfo = TradeInfo.create(
                tradeExposureOnStrategy.getTradeExposure(),
                account,
                originalTrade.getSymbol(), //The accumulation bellow will seek the parent symbol, we keep the original here with it's TradeExposure for clarity / correctness.
                ti.getServerId(),
                originalTrade.getType() == TradeObjectType.TRADE_UPDATE ? TradeType.OpenTrade : TradeMappingUtils.getTradeType(originalTrade.getType()),
                null); //This field is not accessed from anywhere.

            final SymbolExposureEntry initialSymbolExposureEntry = exposureBundle.getSymbolExposures().get(destSymbol);

            final SymbolExposureEntry exposureEntryOld = initialSymbolExposureEntry == null ? null : SymbolExposureEntry.fromSymbolExposureEntry(initialSymbolExposureEntry);
            final SymbolExposureComputedValues exposureComputedValuesOld = exposureEntryOld == null ? null : exposureEntryOld.getAssetExposureComputedValues(); //the first time for each symbol it's null here.

            final Map<String, SubmittedTradeMarker> submittedTradeMarkers = mStrategySubmittedTrades.get(strategyId).get(destSymbol);

            adjustExposures(exposureBundle, weightedTradeInfo, originalTrade.getComment(), submittedTradeMarkers, isClientTrade);

            final SymbolExposureEntry newSymbolExposure = exposureBundle.getSymbolExposures().get(destSymbol);
            final SymbolExposureComputedValues exposureComputedValuesNew = newSymbolExposure.getAssetExposureComputedValues();
            final double indicatorValue = DecisionUtils.calculateIndicatorValue(newSymbolExposure, commonGroupWeight, strategyWeight);

            if (mStartTradingActivity && !mTradeDisabledStrategies.contains(strategyId)) {

                final double sumOfExposures = DecisionUtils.getSumOfExposures(exposureComputedValuesNew);

                final boolean hasPendingTrades = mDecisionController.hasPendingTrades(strategyId, destSymbol);

                checkAndPossiblyChangeSymbolThresholdLimitsType(
                    strategyParameters, exposureComputedValuesNew, strategyId, destSymbol);

                final StrategySymbolLimits symbolLimits =
                    strategyParameters
                        .getSymbolLimits()
                        .get(destSymbol)
                        .get(getCurrentExecutionType(strategyId, destSymbol));

                final double symbolTradingVolumeThreshold = symbolLimits.getTradingThreshold();

                if (hasPendingTrades || shouldWeTrade(sumOfExposures, indicatorValue, indicatorThreshold, symbolTradingVolumeThreshold)) {
                    final double netJfundCoverage = exposureComputedValuesNew.mNetCoverage;

                    final OrderVolumeCalculationResult calculationResult = mDecisionController.calculateOrderVolume(
                        sumOfExposures,
                        netJfundCoverage,
                        destSymbolContractSize,
                        symbolLimits);

                    if (calculationResult.isValid()) {
                        final double volume = calculationResult.getOrderVolume();

                        final int submissionId = mStrategyAntiCoverageIdGenerator.get(strategyId);
                        mStrategyAntiCoverageIdGenerator.put(strategyId, submissionId + 1);

                        final String comment = getAntiCoverageTradeComment(strategyId, submissionId);

                        logger.info("Have a valid volume to execute. Volume: {}, Strategy {}, Symbol {}, Reason: {}, Sum of Exposures: {}, Comment: {}", volume, strategyId, destSymbol, calculationResult.getOrderVolumeCalculationStatus(), sumOfExposures, comment);

                        final double simulatedOpenPrice = getSimulatedOpenPrice(exposureBundle, destSymbol, volume);

                        final BigDecimal accurateVolume = getAccurateVolume(volume, destSymbolContractSize);

                        final double accurateVolumeDouble = accurateVolume.doubleValue();

                        final TradeInfo submittedTradeInfo = accumulateAntiCoverageTrade(exposureBundle, destinationSymbolWithDetails, volume, simulatedOpenPrice, account);

                        final Map<String, SubmittedTradeMarker> submittedTradeRequests = mStrategySubmittedTrades.get(strategyId).get(destSymbol);
                        submittedTradeRequests.put(comment, SubmittedTradeMarker.create(comment, submittedTradeInfo));

                        if (isVolumeSafeJustBeforeSendingForExecution(accurateVolumeDouble, symbolLimits, netJfundCoverage)) {

                            final Side side = accurateVolumeDouble < 0 ? Side.SELL : Side.BUY;
                            final TYPE type = symbolLimits.getType();
                            mTradeOutgoingQueue.add(TradeForExecution.create(strategyId, destSymbol, accurateVolume.abs(), comment, side, type));
                            MDC.put(sLogFileNameParam, LogFileName.DECISION.forStrategy(strategyId));
                            sCsvLogger.info("{}, {}, {}, {}, {}, {} {}", destSymbol, comment, accurateVolumeDouble, String.format("%11.2f", symbolTradingVolumeThreshold), String.format("%11.8f", indicatorValue), String.format("%11.2f", indicatorThreshold), sumOfExposures);
                        }
                        else {
                            final String errorMessage = getErrorMessageForBadVolume(volume, sumOfExposures, destSymbolContractSize, netJfundCoverage, strategyId);
                            if (isProduction) {
                                NotificationUtils.notifyAdmins(NotificationLevel.FULL, errorMessage);
                            }
                            throw new RuntimeException(errorMessage);
                        }
                    }
                }
            }

            logTradeIfNeeded(ti, strategyId, exposureComputedValuesOld, exposureComputedValuesNew, tradeExposureOnStrategy.getRiskGroupName(), indicatorValue);
        });
    }

    BigDecimal getAccurateVolume(final double volume, final double destSymbolContractSize) {
        return BigDecimal.valueOf(volume).multiply(BigDecimal.valueOf(destSymbolContractSize));
    }

    private void checkAndPossiblyChangeSymbolThresholdLimitsType(final StrategyParameters strategyParameters, final SymbolExposureComputedValues exposureComputedValuesNew, final int strategyId, final String destSymbol) {
        final StrategySymbolLimits standardStrategySymbolLimits =
            strategyParameters.getSymbolLimits().get(destSymbol).get(TYPE.STANDARD);

        final double standardTradingThreshold = standardStrategySymbolLimits.getTradingThreshold();

        final double standardDangerExposureLimit = standardStrategySymbolLimits.getDangerExposureLevel();

        final double currentTradingFlow = exposureComputedValuesNew.mNetTrades;
        final double currentAntiCoverage = exposureComputedValuesNew.mNetCoverage;

        final double tradingFlowSign = MathUtils.getSignMultiplier(currentTradingFlow);

        if (DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(
            standardTradingThreshold,
            standardDangerExposureLimit,
            currentTradingFlow,
            currentAntiCoverage,
            tradingFlowSign)
            && isStrategySubmittedTradesZero(strategyId, destSymbol)) {
            if (!isCurrentlyStandardExecution(strategyId, destSymbol)) {
                setCurrentExecutionTypeToStandard(strategyId, destSymbol);
                logger.info(
                    "Strategy symbol limits for strategy {} and for symbol {} has changed to {}.",
                    strategyId,
                    destSymbol,
                    getCurrentExecutionType(strategyId, destSymbol));

                CurrentActiveThresholdTypeAuditHandler.updateCurrentActiveStrategySymbolLimits(
                    strategyId, destSymbol, getCurrentExecutionType(strategyId, destSymbol));
            }
        }
    }

    private boolean isStrategySubmittedTradesZero(final int strategyId, final String destSymbol) {
        return mStrategySubmittedTrades.get(strategyId).get(destSymbol).size() == 0;
    }

    /**
     * Get error message for bad volume just before sending off for execution
     *
     * @param volume             volume to send
     * @param sumOfExposures     sum of exposures
     * @param symbolContractSize contract size for symbol
     * @param netJfundCoverage   net jfund coverage at this time
     * @return error message string
     */
    static String getErrorMessageForBadVolume(final double volume, final double sumOfExposures, final double symbolContractSize, final double netJfundCoverage, final int strategyId) {
        return String.format("JFUND, Something is wrong with our calculations. We need to investigate. Strategy: %d, Order volume: %f, Sum of exposures: %f, Net jfund coverage: %f, symbol contract size: %f", strategyId, volume, sumOfExposures, netJfundCoverage, symbolContractSize);
    }

    /**
     * This is the last check we do before sending the trade off for execution
     *
     * @param volumeWithContractSize           volume we want to trade
     * @param limits           strategy limits
     * @param netJfundCoverage the current net jfund coverage we have
     * @return true if volume is safe, false if not
     */
    static boolean isVolumeSafeJustBeforeSendingForExecution(final double volumeWithContractSize, final StrategySymbolLimits limits, final double netJfundCoverage) {
        final DangerCollector collector = DangerCollector.create();
        final List<Danger> dangers = collector.getDangers(limits, volumeWithContractSize, netJfundCoverage);

        return dangers.isEmpty() && !MathUtils.isZero(volumeWithContractSize);
    }

    /**
     * Should we trade now?
     * We should trade if we've met the indicator threshold and the net customer trades have exceeded our threshold
     *
     * @param sumOfExposures     net client exposures or trades
     * @param indicatorValue     indicator value
     * @param indicatorThreshold threshold for indicator value
     * @param tradingThreshold   threshold of when we can start trading
     * @return true if our trading thresholds are met
     */
    private boolean shouldWeTrade(final double sumOfExposures, final double indicatorValue, final double indicatorThreshold, final double tradingThreshold) {
        return DecisionUtils.exposuresExceedIndicatorThreshold(indicatorValue, indicatorThreshold) && DecisionUtils.exposuresExceedSymbolThreshold(sumOfExposures, tradingThreshold);
    }

    private String getAntiCoverageTradeComment(final int strategyId, final int submissionId) {

        final String submissionComment = String.format("%d%d%d", strategyId, System.nanoTime(), submissionId);

        if (submissionComment.length() > Mt4ArraySizes.sTradeRecord_comment_max_size) {
            NotificationUtils.notifyAdmins(NotificationLevel.FULL, String.format("Strategy: %d, created a trade submission with comment: %s which is above the limit of: %s. Please investigate", strategyId, submissionComment, Mt4ArraySizes.sTradeRecord_comment_max_size));
        }

        return submissionComment.substring(0, Math.min(Mt4ArraySizes.sTradeRecord_comment_max_size, submissionComment.length()));
    }

    private void logTradeImpact(
        final int strategyId,
        final TradeObject trade,
        final int serverId,
        final String riskGroupName,
        final SymbolExposureComputedValues expOld,
        final SymbolExposureComputedValues expNew,
        final double indicatorValue) {

        MDC.put(sLogFileNameParam, LogFileName.ACTIVITY.forStrategy(strategyId));
        sCsvLogger.info("{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
            serverId,
            trade.getId(),
            trade.getType(),
            trade.getSymbol(),
            trade.isBuy() ? "buy" : "sell",
            (trade.getVolume()),
            //TODO Merge into a single execution timestamp instead of open and close times
            trade.getExecutedTimestamp(),
            trade.getExecutedTimestamp(),
            riskGroupName,
            (int) indicatorValue,
            expOld == null ? "N/A" : expOld.mNetTrades,
            expNew.mNetTrades,
            expOld == null ? "N/A" : expOld.mNetCoverage,
            expNew.mNetCoverage,
            expNew.mNetTrades + expNew.mNetCoverage);
    }

    private void logTradeIfNeeded(
        final TradeInfoWithStrategyExposures ti,
        final int strategyId,
        final SymbolExposureComputedValues exposureComputedValuesOld,
        final SymbolExposureComputedValues exposureComputedValuesNew,
        final String riskGroupName,
        final double indicatorValue) {

        final boolean tradeImpactedExposures = hasTradeImpact(exposureComputedValuesOld, exposureComputedValuesNew);
        if (mLogExposuresMode == LogExposuresMode.FULL && tradeImpactedExposures) {
            logTradeImpact(strategyId, ti.getTrade(), ti.getServerId(), riskGroupName, exposureComputedValuesOld, exposureComputedValuesNew, indicatorValue);
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    private void populateConversionFactors() {
        final Future<?> f0 = mExecutorService.submit(this::populateConversionsFactorsForCurrencies);
        final Future<?> f1 = mExecutorService.submit(this::populateConversionsFactorsForSymbols);
        final Future<?> f2 = mExecutorService.submit(this::populateQuotesForSymbols);

        try {
            f0.get();
            f1.get();
            f2.get();
        }
        catch (final Throwable ex) {
            final String errString = "Exception while waiting for the worker threads to finish.";
            logger.error(errString, ex);//TODO: This email either remove or rate limit!!
            EmailSender.notifyAdmins(JAnalystEmailCredentials.get(), "JFund", StringUtils.makeErrorMessage(errString, ex));
        }
    }

    private void populateConversionsFactorsForSymbols() {

        mSymbolConversionFactors.clear();

        mStrategyExposureMap.values().forEach(e -> e.populateSymbolConversionFactors(mSymbolConversionFactors, mConversionPump));
    }

    private void populateConversionsFactorsForCurrencies() {

        mCurrencyConversionFactors.clear();

        mStrategyExposureMap.values().forEach(e -> e.populateCurrencyConversionFactors(mCurrencyConversionFactors, mConversionPump));
    }

    /**
     * Fetches quotes from ConversionPump and sets the mFxQuote field of the symbol exposures. Does not apply to currency exposures.
     */
    private void populateQuotesForSymbols() {

        final List<String> notFoundSymbols = new ArrayList<>();

        mStrategyExposureMap.values().forEach(e -> {

            // Symbol Exposures
            final Map<String, SymbolExposureEntry> symbolExposures = e.getSymbolExposures();

            symbolExposures.forEach((symbol, value) -> {

                try {
                    final TickInfoInternal ti = mConversionPump.getTickInfo(symbol);
                    value.setBid(ti.getBid());
                    value.setAsk(ti.getAsk());
                }
                catch (final NoSuchSymbolTickException ex) {
                    // do not rethrow, we need to collect all symbols that are not found and then
                    // log the error
                    notFoundSymbols.add(symbol);
                }
            });

            if (!notFoundSymbols.isEmpty() && !mFXQuoteSymbolErrorOccurred) {
                //TODO: Is this logWarning or a possible e-mail relevant? logWarning(sc, "populateQuotesForSymbols", errString);
                mFXQuoteSymbolErrorOccurred = true;
            }
        });
    }

    private static String getParentSymbol(final String symName, final SymbolModule.SymbolCache symbolCache) {
        final SymbolWithDetails symbolWithDetails = symbolCache.get(symName);
        final String parentSymbol = symbolWithDetails.getSymbolDetails().getParentSymbol();

        return (parentSymbol != null ? symbolCache.get(parentSymbol) : symbolWithDetails).getSymbolName();
    }

    private void sendExposuresToTheEther(final LocalDateTime exposureDateTime) {

        if (mStartTradingActivity) {
            mStrategyExposureMap.forEach((strategyId, exposureBundle) -> {
                if (!mTradeDisabledStrategies.contains(strategyId)) {

                    final Map<String, CurrencyExposureEntry> currencyExposures = exposureBundle.getCurrencyExposures();
                    final Map<String, SymbolExposureEntry> symbolExposures = exposureBundle.getSymbolExposures();

                    final RiskGroupExposure data =
                        RiskGroupExposure.create(currencyExposures,
                            symbolExposures,
                            exposureDateTime);

                    if (mLogExposuresMode == LogExposuresMode.ACCUMULATION) {
                        sendSymbolExposuresForLogging(symbolExposures, exposureDateTime, strategyId, mStrategyParametersMap.get(strategyId).getExecutionInfo().getStrategyWeight());
                    }

                    insertToZabbixHandlerQueue(strategyId, symbolExposures);
                    insertToExposureSenderQueue(strategyId, data);
                }
            });
        }
    }

    private void sendSymbolExposuresForLogging(final Map<String, SymbolExposureEntry> symbolExposures, final LocalDateTime exposureDateTime, final int strategyId, final double strategyWeight) {

        symbolExposures.values()
            .forEach(u -> {
                    final SymbolExposureComputedValues values = u.getAssetExposureComputedValues();

                    MDC.put(sLogFileNameParam, LogFileName.ACCUMULATION.forStrategy(strategyId));

                    sCsvLogger.info("{}, {}, {}, {}, {}, {},{},{}", exposureDateTime,
                        u.mSymbolName,
                        String.format("%.2f", u.mTradeExp.mLongExp),
                        String.format("%.2f", u.mTradeExp.mShortExp),
                        String.format("%.2f", values.mNetTrades),
                        String.format("%.2f", u.mCoverage.mLongExp),
                        String.format("%.2f", u.mCoverage.mShortExp),
                        String.format("%.2f", values.mNetCoverage),
                        String.format("%.2f", -values.mNetCoverage * strategyWeight - values.mNetTrades));
                }
            );
    }

    private static final int sTimeBetweenExposureShipments = 1000;

    private void scheduleNextExposureShipment() {
        mTimeToSendMyWorkOnwards = false;
        mExposureShipmentScheduler.schedule(
            () -> {
                mTimeToSendMyWorkOnwards = true;
            },
            sTimeBetweenExposureShipments,
            TimeUnit.MILLISECONDS);
    }

    private void insertToZabbixHandlerQueue(final int strategyId, final Map<String, SymbolExposureEntry> symbolExposureMap) {

        for (final Entry<String, SymbolExposureEntry> entry : symbolExposureMap.entrySet()) {
            final String symbolName = entry.getKey();
            final SymbolExposureEntry exposureEntry = entry.getValue();

            final SymbolExposureComputedValues exposureComputedValues = exposureEntry.getAssetExposureComputedValues();

            mZabbixMessageQueue.add(TradeMessage.create(mZabbixRequestTrapperName, strategyId, symbolName, Math.abs(DecisionUtils.getSumOfExposures(exposureComputedValues))));
            mZabbixMessageQueue.add(ExposureMessage.create(mZabbixExposureTrapperName, strategyId, symbolName, Math.abs(exposureComputedValues.mNetCoverage)));
        }
    }

    private void insertToExposureSenderQueue(final int strategyId, final RiskGroupExposure riskGroupExposure) {

        final BlockingQueue<byte[]> strategyQueue = mStrategyToExposureSenderQueueMap.get(strategyId);

        strategyQueue.clear();
        strategyQueue.add(riskGroupExposure.serialize(mGoogleJson, mXMDeflater));
    }

    void setCurrentExecutionTypeToStandard(final int strategyId, final String symbol) {
        mCurrentExecutionSymbolLimitTypePerSymbolPerStrategy.get(strategyId).put(symbol, TYPE.STANDARD);
    }

    TYPE getCurrentExecutionType(final int strategyId, final String symbol) {
        return mCurrentExecutionSymbolLimitTypePerSymbolPerStrategy.get(strategyId).get(symbol);
    }

    boolean isCurrentlyStandardExecution(final int strategyId, final String symbol) {
        return mCurrentExecutionSymbolLimitTypePerSymbolPerStrategy.get(strategyId).get(symbol)
            == TYPE.STANDARD;
    }

    private static void verifyStrategySymbolLimitsExecutionTypes
        (final Map<Integer, StrategyParameters> strategyParametersMap) {

        for (final Integer strategyId : strategyParametersMap.keySet()) {
            for (final String symbol : strategyParametersMap.get(strategyId).getSymbolLimits().keySet()) {
                if (!strategyParametersMap
                    .get(strategyId)
                    .getSymbolLimits()
                    .get(symbol)
                    .containsKey(TYPE.STANDARD)) {
                    throw new RuntimeException(
                        String.format(
                            "Strategy %s for Symbol %s MUST contain a STANDARD Strategy Symbol Threshold.",
                            strategyId, symbol));
                }
            }
        }
    }

    private static Map<Integer, Map<String, TYPE>> initialiseSymbolStrategyLimitCurrentState
        (final Map<Integer, StrategyParameters> strategyParametersMap) {

        verifyStrategySymbolLimitsExecutionTypes(strategyParametersMap);

        final Map<Integer, Map<String, TYPE>> currentExecutionSymbolLimitTypePerSymbolPerStrategy =
            new HashMap<>();
        for (final Integer strategyId : strategyParametersMap.keySet()) {
            currentExecutionSymbolLimitTypePerSymbolPerStrategy.put(strategyId, new HashMap<>());
            for (final String symbol : strategyParametersMap.get(strategyId).getSymbolLimits().keySet()) {
                if (strategyParametersMap
                    .get(strategyId)
                    .getSymbolLimits()
                    .get(symbol)
                    .containsKey(TYPE.CATCH_UP)) {
                    currentExecutionSymbolLimitTypePerSymbolPerStrategy
                        .get(strategyId)
                        .put(symbol, TYPE.CATCH_UP);
                }
                else if (strategyParametersMap
                    .get(strategyId)
                    .getSymbolLimits()
                    .get(symbol)
                    .containsKey(TYPE.STANDARD)) {
                    currentExecutionSymbolLimitTypePerSymbolPerStrategy
                        .get(strategyId)
                        .put(symbol, TYPE.STANDARD);
                }

                else {
                    throw new RuntimeException("Symbol Strategy Limits Type requested not found in the archives. BYE!");
                }
            }
        }

        return currentExecutionSymbolLimitTypePerSymbolPerStrategy;
    }

    private ExposuresCollector(
        final DecisionController decisionController,
        final Map<Integer, StrategyParameters> strategyParametersMap,
        final BlockingDeque<ExposuresMessage> incomingQueue,
        final CurrencyConversionPump conversionPump,
        final BlockingQueue<TradeForExecution> tradeOutgoingQueue,
        final SymbolModule.SymbolCache symbolCache,
        final TradeInfoWithStrategyExposures sentinelTradeInfoWithStrategyExposures,
        final LogExposuresMode logExposuresMode,
        final boolean timeToDie,
        final boolean startTradingActivity,
        final ExecutorService executorService,
        final ScheduledExecutorService exposureShipmentScheduler,
        final Map<Integer, ExposureBundle> strategyExposureBundleMap,
        final Gson googleJson,
        final boolean fxQuoteSymbolErrorOccurred,
        final XMDeflater xmDeflater,
        final Map<String, Double> symbolConversionFactors,
        final Map<String, Double> currencyConversionFactors,
        final BlockingQueue<ZabbixMessage> zabbixMessageQueue,
        final String zabbixExposureTrapperName,
        final String zabbixRequestTrapperName,
        final Map<Integer, BlockingQueue<byte[]>> strategyToExposureSenderQueueMap,
        final Map<Integer, Integer> strategyAntiCoverageIdGenerator,
        final Map<Integer, Map<String, Map<String, SubmittedTradeMarker>>> strategySubmittedTrades,
        final Set<Integer> tradeDisabledStrategies,
        final Map<Integer, Map<String, TYPE>> currentExecutionSymbolLimitTypePerSymbolPerStrategy,
        final boolean isProduction) {

        mDecisionController = decisionController;

        mStrategyParametersMap = strategyParametersMap;

        mIncomingQueue = incomingQueue;

        mConversionPump = conversionPump;
        mTradeOutgoingQueue = tradeOutgoingQueue;

        mSymbolCache = symbolCache;
        mSentinelTradeInfoWithStrategyExposures = sentinelTradeInfoWithStrategyExposures;

        mLogExposuresMode = logExposuresMode;

        mTimeToDie = timeToDie;
        mStartTradingActivity = startTradingActivity;
        mExecutorService = executorService;
        mExposureShipmentScheduler = exposureShipmentScheduler;
        mStrategyExposureMap = strategyExposureBundleMap;
        mGoogleJson = googleJson;
        mFXQuoteSymbolErrorOccurred = fxQuoteSymbolErrorOccurred;
        mXMDeflater = xmDeflater;

        mSymbolConversionFactors = symbolConversionFactors;
        mCurrencyConversionFactors = currencyConversionFactors;

        mZabbixMessageQueue = zabbixMessageQueue;
        mZabbixExposureTrapperName = zabbixExposureTrapperName;
        mZabbixRequestTrapperName = zabbixRequestTrapperName;

        mStrategyToExposureSenderQueueMap = strategyToExposureSenderQueueMap;

        mStrategyAntiCoverageIdGenerator = strategyAntiCoverageIdGenerator;
        mStrategySubmittedTrades = strategySubmittedTrades;

        mTradeDisabledStrategies = tradeDisabledStrategies;

        mCurrentExecutionSymbolLimitTypePerSymbolPerStrategy = currentExecutionSymbolLimitTypePerSymbolPerStrategy;
        this.isProduction = isProduction;
    }

    private final DecisionController mDecisionController;
    private final Map<Integer, StrategyParameters> mStrategyParametersMap;

    private final BlockingDeque<ExposuresMessage> mIncomingQueue;

    private final CurrencyConversionPump mConversionPump;

    private final BlockingQueue<TradeForExecution> mTradeOutgoingQueue;
    private volatile boolean mTimeToDie;
    private boolean mStartTradingActivity;
    private final SymbolModule.SymbolCache mSymbolCache;
    private final TradeInfoWithStrategyExposures mSentinelTradeInfoWithStrategyExposures;

    private final ExecutorService mExecutorService;

    private final Gson mGoogleJson;

    private final ScheduledExecutorService mExposureShipmentScheduler;
    private final Map<Integer, ExposureBundle> mStrategyExposureMap;

    private volatile boolean mTimeToSendMyWorkOnwards;

    private boolean mFXQuoteSymbolErrorOccurred;
    private final XMDeflater mXMDeflater;

    private final LogExposuresMode mLogExposuresMode;

    private final Map<String, Double> mSymbolConversionFactors;
    private final Map<String, Double> mCurrencyConversionFactors;

    private final BlockingQueue<ZabbixMessage> mZabbixMessageQueue;
    private final String mZabbixExposureTrapperName;
    private final String mZabbixRequestTrapperName;

    private final Map<Integer, BlockingQueue<byte[]>> mStrategyToExposureSenderQueueMap;

    private final Map<Integer, Integer> mStrategyAntiCoverageIdGenerator;
    private final Map<Integer, Map<String, Map<String, SubmittedTradeMarker>>> mStrategySubmittedTrades;

    private final Set<Integer> mTradeDisabledStrategies;

    private final Map<Integer, Map<String, TYPE>> mCurrentExecutionSymbolLimitTypePerSymbolPerStrategy;
    private final boolean isProduction;
}