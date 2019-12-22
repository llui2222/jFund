package com.xm.jfund.exposures;

import com.xm.jfund.queuemessages.ExposuresMessage;
import com.xm.jfund.queuemessages.TradeInfoWithStrategyExposures;
import com.xm.jfund.riskgroup.RiskGroupWithExpFactor;
import com.xm.jfund.trade.TradeObject;
import com.xm.jfund.trade.TradeObjectFactory;
import com.xm.jfund.trade.TradeObjectType;
import com.xm.jfund.utils.StrategyAffectingRiskGroup;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.TradeExposureInfo;
import com.xm.jfund.utils.TradeExposureOnStrategy;
import jAnalystUtils.AnalystPumpingManagerModule.AnalystMt4PumpingManager;
import jAnalystUtils.SymbolModule;
import jAnalystUtils.SymbolModule.SymbolCache;
import jAnalystUtils.SymbolModule.SymbolMetaData;
import jAnalystUtils.SymbolModule.SymbolWithDetails;
import jAnalystUtils.TradeExposure;
import jAnalystUtils.UserCache;
import jAnalystUtils.UserCache.UserInfo;
import jAnalystUtils.riskGroups.RiskGroup;
import jxmUtils.Functionals;
import jxmUtils.Functionals.BooleanIntObjObjConsumer;
import jxmUtils.ServerFarm;
import jxmUtils.ServerFarm.ServerDetails;
import jxmUtils.TupleModule.Pair;
import mt4j.MessageModule;
import mt4j.MessageModule.ConnectionEstablished;
import mt4j.MessageModule.ConnectionLost;
import mt4j.MessageModule.Mt4Message;
import mt4j.MessageModule.SymbolTickOccured;
import mt4j.MessageModule.SymbolUpdated;
import mt4j.MessageModule.TradeClosed;
import mt4j.MessageModule.TradeDeletedByAdmin;
import mt4j.MessageModule.TradeOpened;
import mt4j.MessageModule.TradeUpdated;
import mt4j.MessageVisitorsModule;
import mt4j.PumpingManager;
import mt4j.ServerException;
import mt4jUtils.Symbol;
import mt4jUtils.TickInfo;
import mt4jUtils.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import platformObjects.interfaces.TickInformation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author nmichael
 */
public final class MessageProcessor extends MessageVisitorsModule.ShunningMessageVisitor implements IMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    /**
     * @param serverDetails                 Details of the server that this MessageProcessor is listening to.
     * @param collectorOutgoingQueue        Queue that messages will be written to.
     * @param login                         User login.
     * @param password                      User password.
     * @param messageRequest                Field that indicates what MT4 messages to receive.
     * @param strategyParametersMap         A mapping from the strategy id to it's immutable parameters
     * @param countDownLatchForServers      Used to synchronize the MessageProcessor threads and the ExposureCollectorThread.
     * @param countDownLatchForCaller       Used to synchronize the MessageProcessor threads and the ExposureCollectorThread.
     * @param userCache                     The userCache.
     * @param symbolCache                   The symbolCache.
     * @param strategySymbolsInPlay         A Mapping from the strategy id to a mapping of symbol names that it tracks to their metadata.
     * @param strategyLpToSymbolNameMapping A mapping from the special LP symbol names to the normal ones (e.g EURUSDx -> EURUSD), per strategy.
     *                                      There is an assumption that the mapping is 1-1 across strategies, so it could potentially be refactored into a single Map<String, String>
     *                                      If we don't mind fetching all the Mapping data from the DB.
     * @param openTradesToExposureMap       A mapping from the open trades known to the system to their exposure information.
     *                                      TradeObject update messages generate adjustments and this map is updated with the delta
     *                                      so we can close a trade on the same exposure that it currently has.
     * @param tickInfoQueue                 queue for tick information, null if not needed
     **/
    public static MessageProcessor create(
        final ServerFarm.ServerDetails serverDetails,
        final BlockingQueue<ExposuresMessage> collectorOutgoingQueue,
        final int login,
        final String password,
        final int messageRequest,
        final Map<Integer, StrategyParameters> strategyParametersMap,
        final CountDownLatch countDownLatchForServers,
        final CountDownLatch countDownLatchForCaller,
        final UserCache userCache,
        final SymbolCache symbolCache,
        final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay,
        final Map<Integer, Map<String, String>> strategyLpToSymbolNameMapping,
        final Map<Integer, TradeExposureInfo> openTradesToExposureMap,
        final BlockingQueue<TickInformation> tickInfoQueue) {

        final BlockingQueue<MessageModule.Mt4Message> mMt4Queue = new LinkedBlockingQueue<>();

        final PumpingManager pumpingManager = createConnection(login, password, serverDetails.getServerId(), serverDetails.getHostName(), messageRequest, mMt4Queue);

        return new MessageProcessor(
            serverDetails,
            mMt4Queue,
            pumpingManager,
            collectorOutgoingQueue,
            strategyParametersMap,
            countDownLatchForServers,
            countDownLatchForCaller,
            userCache,
            symbolCache,
            strategySymbolsInPlay,
            strategyLpToSymbolNameMapping,
            openTradesToExposureMap,
            tickInfoQueue);
    }

    @Override
    public void run() {

        final int serverId = mServerDetails.getServerId();
        logger.info("Message processor for server: {} has started...", serverId);
        try {
            while (!mTimeToDie) {

                final Optional<Mt4Message> msgOpt = getNextMessage();

                msgOpt.ifPresent(mt4Message -> mt4Message.accept(this));
            }
        }
        finally {
            // These are here because it is possible to lose the connection immediately after
            // succesfully connecting (i.e. the ConnectionLost message comes before any
            // ConnectionEstablished message comes).  In this case we don't want to block
            // all the other threads (processors & main thread) from continuing on.  This
            // thread will throw an exception which will cause recovery to happen.
            // It is harmless to countDown if you are already at zero (i.e. all ConnectionEstablished
            // messages happened first).
            mCountDownLatchForServers.countDown();
            mCountDownLatchForCaller.countDown();
            logger.info("Message processor for server: {} has ended.", serverId);
        }
    }

    private static final int sPollingTimeForIncomingMessages = 800;

    private Optional<Mt4Message> getNextMessage() {
        try {
            return Optional.ofNullable(mMt4Queue.poll(sPollingTimeForIncomingMessages, TimeUnit.MILLISECONDS));
        }
        catch (final InterruptedException ex) {
            final String errString = "The thread was interrupted while polling for messages.";
            logger.error(errString, ex);
            throw new RuntimeException(errString, ex);
        }
    }

    @Override
    public void timeToDie() {
        mTimeToDie = true;
    }

    public PumpingManager getPumpingManager() {
        return mPumpingManager;
    }

    @Override
    public void cleanup() {
        mPumpingManager.disconnect();
    }

    public ServerDetails getServerDetails() {
        return mServerDetails;
    }

    public String getServerName() {
        return String.format("Server(%s, %d)", mServerDetails.getServerName(), mServerDetails.getServerId());
    }

    @Override
    public boolean connect() {
        return mPumpingManager.connect();
    }

    @Override
    public void disconnect() {
        mPumpingManager.disconnect();
        mMt4Queue.clear();
    }

    private static PumpingManager createConnection(
        final int login,
        final String password,
        final int serverId,
        final String ip,
        final int messageRequest,
        final BlockingQueue<Mt4Message> queue) {

        return PumpingManager.create(login, password, serverId, ip, messageRequest, queue);
    }

    private String getParentSymbol(final String symbol) {
        return mParentSymbolsCache.computeIfAbsent(symbol, SymbolModule::getNormalizedSymbolName);
    }

    /*The assumption here is that the symbol mappings are 1-1 across strategies*/
    private Optional<String> getMappedName(final String tradeSymbol) {

        return mStrategyLpToSymbolNameMapping.values().stream()
            .flatMap(symbolMap -> {
                final String mappedName = symbolMap.get(tradeSymbol);

                return (mappedName != null && !tradeSymbol.equals(mappedName))
                    ? Stream.of(mappedName)
                    : Stream.empty();
            })
            .findAny();
    }

    private void process(final int serverId, final Trade trade, final BooleanIntObjObjConsumer<Trade, List<StrategyAffectingRiskGroup>> process) {

        final String tradeSymbol = getParentSymbol(trade.mSymbol);

        final Optional<String> mappedNameOpt = getMappedName(tradeSymbol);

        final Map<Integer, List<RiskGroupWithExpFactor>> strategiesThatTrackSymbolToRiskGroupMap = mStrategyParametersMap.entrySet().stream()
            .filter(entry -> isRelevantSymbol(tradeSymbol, mappedNameOpt, entry.getKey()))
            .map(entry -> Pair.create(entry.getKey(), entry.getValue().getRiskGroupsWithExposureFactors()))
            .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));

        if (strategiesThatTrackSymbolToRiskGroupMap.size() > 0) {

            final List<StrategyAffectingRiskGroup> strategyAffectingRiskGroups = strategiesThatTrackSymbolToRiskGroupMap.entrySet().stream()
                .flatMap(entry -> {
                    final int strategyId = entry.getKey();
                    final List<RiskGroupWithExpFactor> strategyRiskGroups = entry.getValue();

                    final Optional<RiskGroupWithExpFactor> memberRiskGroupOpt = strategyRiskGroups.stream()
                        .filter(r -> {
                            final RiskGroup riskGroup = r.getRiskGroup();
                            final boolean isMember = riskGroup.isMember(serverId, trade.mLogin, mUserInfoBiFunction) && isRelevantVolume(trade, strategyId);
                            final Set<Integer> reasonCodes = mStrategyParametersMap.get(strategyId).getExecutionInfo().getReasonCodes();
                            return isMember && isRelevantReason(trade.getReason(), reasonCodes);
                        })
                        .findAny(); //The assumption that we have to make is that an account belongs to only one tracked risk group per strategy.

                    return Functionals.mapOpt(memberRiskGroupOpt,
                        memberRiskGroup -> Stream.of(StrategyAffectingRiskGroup.create(strategyId, memberRiskGroup)),
                        Stream::empty);
                })
                .collect(Functionals.toArrayList());

            if (!strategyAffectingRiskGroups.isEmpty()) {

                final Trade tradeWithNormalizedName = Functionals.mapOpt(mappedNameOpt, s -> Trade.createTradeWithSymbol(trade, s), () -> trade);

                process.accept(false, serverId, tradeWithNormalizedName, strategyAffectingRiskGroups);
            }
        }
    }

    private boolean isRelevantVolume(final Trade trade, final int strategyId) {

        final int tradeVolume = trade.getVolume();
        final double contractSize = getContractSize(trade.getSymbol());

        return (tradeVolume * contractSize / 100) >= mStrategyParametersMap.get(strategyId).getExecutionInfo().getTradeVolumeThreshold();
    }

    static boolean isRelevantReason(final int reason, final Set<Integer> reasonCodes) {
        final boolean isRelevant;
        if (!reasonCodes.isEmpty()) {
            isRelevant = !reasonCodes.contains(reason);
        }
        else {
            isRelevant = true;
        }
        return isRelevant;
    }

    private boolean isRelevantSymbol(final String symbolName, final Optional<String> mappedNameOpt, final Integer strategyId) {

        return Functionals.mapOpt(mappedNameOpt, name -> symbolIsTracked(name, strategyId), () -> false)
            || symbolIsTracked(symbolName, strategyId);
    }

    private boolean symbolIsTracked(final String tradeSymbol, final Integer strategyId) {

        if (mStrategySymbolsInPlay.get(strategyId).containsKey(tradeSymbol)) {
            return true;
        }
        else {
            final Optional<SymbolMetaData> symbolMetaDataOpt = SymbolModule.getSymbolMetaDataByName(tradeSymbol);

            return symbolMetaDataOpt.isPresent() && mStrategySymbolsInPlay.get(strategyId).containsKey(symbolMetaDataOpt.get().getParentSymbol());
        }
    }

    @Override
    public void visit(final ConnectionEstablished m) {
        final int serverId = getPumpingManager().getServerId();
        logger.info("Successfully connected to server {}.", serverId);

        boolean cachePopulateSucceded = false;
        try {
            final List<platformObjects.interfaces.Symbol> symbolList = mPumpingManager.getAllSymbols().stream()
                .map(mt4j.platformObjects.Symbol::create)
                .collect(Functionals.toArrayList());

            mSymbolCache.populateFromServerGivenSymbols(symbolList);
            cachePopulateSucceded = true;
        }
        catch (final ServerException ex) {
            // An exception occurred while fetching symbols.
            // The ConnectionLost message will be send next so there
            // is not much we need to do here.

            cachePopulateSucceded = false;
        }
        finally {
            mCountDownLatchForServers.countDown();
            try {
                mCountDownLatchForServers.await();
            }
            catch (final InterruptedException ex) {
                logger.error("The thread was interrupted while waiting on the countdown latch.", ex);
                throw new RuntimeException(ex);
            }

            if (cachePopulateSucceded) {
                try {
                    processAllOpenTrades();
                }
                catch (final ServerException ex) {
                    // See comment above.  Nothing we can do if the connection was lost.
                }
            }
        }

        mCountDownLatchForCaller.countDown();
    }

    @Override
    public void visit(final ConnectionLost m) {
        final int serverId = getPumpingManager().getServerId();
        logger.error("The connection to server {} has been lost.", serverId);

        throw new MessageProcessorConnectionLostException(String.format("Connection to %s lost.  Will attempt to reconnect...", mServerDetails.getServerName()));
    }

    @Override
    public void visit(final TradeOpened m) {

        process(m.getServerId(), m.getOpenedTrade(), mProcessTradeOpened);
    }

    @Override
    public void visit(final TradeClosed m) {

        process(m.getServerId(), m.getClosedTrade(), mProcessTradeClosed);
    }

    @Override
    public void visit(final TradeDeletedByAdmin m) {

        process(m.getServerId(), m.getDeletedTrade(), mProcessTradeDeletedByAdmin);
    }

    @Override
    public void visit(final TradeUpdated m) {

        process(m.getServerId(), m.getUpdatedTrade(), mProcessTradeUpdate);
    }

    @Override
    public void visit(final SymbolUpdated m) {
        processSymbolUpdated(m.getSymbol());
    }

    @Override
    public void visit(final SymbolTickOccured m) {
        processSymbolTickOccured(m.getTickInfo());
    }

    // Actual implementations below.
    private void processAllOpenTrades() throws ServerException {

        final List<Trade> trades = mPumpingManager.getAllOpenTrades();

        final int serverId = mPumpingManager.getServerId();
        trades.forEach(trade -> process(serverId, trade, mProcessTradeOpened));
    }

    private void addToQueue(
        final List<TradeExposureOnStrategy> tradeExposureOnStrategies,
        final Trade trade,
        final int serverId,
        final TradeObjectType tradeType) {

        final TradeObject tradeObject = TradeObjectFactory.create(trade, tradeType, getContractSize(trade.getSymbol()));
        final TradeInfoWithStrategyExposures tradeInfoWithStrategyExposures = TradeInfoWithStrategyExposures.create(tradeExposureOnStrategies, tradeObject, serverId);

        mModelOutgoingQueue.add(tradeInfoWithStrategyExposures);
    }

    private void processTradeOpened(final boolean isIgnored, final int serverId, final Trade trade, final List<StrategyAffectingRiskGroup> strategyAffectingRiskGroups) {
        final int cmd = trade.mCmd;
        final int order = trade.mOrder;

        if (cmd == Trade.OP_BALANCE || cmd == Trade.OP_CREDIT) {
            final String errString = "Should not encounter BALANCE or CREDIT trades here.";
            logger.error(errString);
            throw new RuntimeException(errString);
        }
        else if (cmd == Trade.OP_BUY || cmd == Trade.OP_SELL) {

            final TradeExposureInfo tradeExposureInfo = mOpenTrades.get(trade.mOrder);

            if (tradeExposureInfo == null) {

                final List<TradeExposureOnStrategy> tradeExposureOnStrategies = createTradeExposureOnStrategies(trade, strategyAffectingRiskGroups);

                Collections.shuffle(tradeExposureOnStrategies);

                mOpenTrades.put(order, TradeExposureInfo.create(cmd, tradeExposureOnStrategies));

                addToQueue(tradeExposureOnStrategies, trade, serverId, TradeObjectType.OPEN_TRADE);
            }
            else {
                logger.warn("Opened trade seen twice. Trade was: {}", trade.toString());
            }
        }
        else {// Pending order.
            mOpenTrades.put(order, TradeExposureInfo.create(cmd, Collections.emptyList()));
        }
    }

    private void processTradeClosed(final boolean isIgnored, final int serverId, final Trade trade, final List<StrategyAffectingRiskGroup> strategyAffectingRiskGroups) {
        final int cmd = trade.mCmd;
        final int order = trade.mOrder;

        if (cmd == Trade.OP_BUY || cmd == Trade.OP_SELL) {

            final TradeExposureInfo p = mOpenTrades.get(order);

            if (p != null) {

                mOpenTrades.remove(order);

                final List<TradeExposureOnStrategy> tradeExposureOnStrategies = p.getTradeExposureOnStrategies();

                if (!tradeExposureOnStrategies.isEmpty()) {
                    addToQueue(tradeExposureOnStrategies, trade, serverId, TradeObjectType.CLOSE_TRADE);
                }
            }
            else {
                logger.warn("Closing trade never seen before.  TradeObject was: {}", trade.toString());
            }
        }
        else if (cmd == Trade.OP_BALANCE || cmd == Trade.OP_CREDIT) {
            final String errString = "Should not encounter BALANCE or CREDIT trades here.";
            logger.error(errString);
            throw new RuntimeException(errString);
        }
        else {// A pending trade is being closed.  Let it go.
            mOpenTrades.remove(order);
        }
    }

    private void processTradeDeletedByAdmin(final boolean isIgnored, final int serverId, final Trade trade, final List<StrategyAffectingRiskGroup> riskGroup) {
        // When a trade is deleted by an admin a closed trade call is made prior to this one.
        // The processing for that trade has been taken cared of there.
    }

    @SuppressWarnings("empty-statement")
    private void processTradeUpdate(final boolean isIgnored, final int serverId, final Trade trade, final List<StrategyAffectingRiskGroup> strategyAffectingRiskGroups) {
        final int order = trade.mOrder;
        final int cmd = trade.mCmd;

        final TradeExposureInfo oldTradeExposureInfo = mOpenTrades.get(order);

        if (oldTradeExposureInfo != null) {

            final List<TradeExposureOnStrategy> tradeExposureOnStrategies = createTradeExposureOnStrategies(trade, strategyAffectingRiskGroups);
            Collections.shuffle(tradeExposureOnStrategies);

            if (Trade.isPendingOrder(oldTradeExposureInfo.getCmd()) && trade.isBuyOrSell()) { // Activation of pending order
                mOpenTrades.put(order, TradeExposureInfo.create(cmd, tradeExposureOnStrategies));

                addToQueue(tradeExposureOnStrategies, trade, serverId, TradeObjectType.OPEN_TRADE);
            }
            else if (!oldTradeExposureInfo.getTradeExposureOnStrategies().isEmpty() && isTradeExposureDifferent(oldTradeExposureInfo.getTradeExposureOnStrategies(), tradeExposureOnStrategies)) { // Otherwise it's an open trade that is being modified.

                // We should update it's exposure for when it eventually closes.
                mOpenTrades.put(order, TradeExposureInfo.create(cmd, tradeExposureOnStrategies));

                //And we should send adjustments for the running strategies
                final List<TradeExposureOnStrategy> adjustmentExposuresOnStrategies = generateAdjustmentExposuresOnStrategies(oldTradeExposureInfo.getTradeExposureOnStrategies(), tradeExposureOnStrategies);

                if (!adjustmentExposuresOnStrategies.isEmpty()) {
                    addToQueue(adjustmentExposuresOnStrategies, trade, serverId, TradeObjectType.TRADE_UPDATE);
                }
            }
            //We don't care about modifications that have no impact on TradeExposure, so we do nothing.
        }
        else if (cmd == Trade.OP_BALANCE || cmd == Trade.OP_CREDIT) {
            // We don't care about balance and credit transactions.
        }
        else {
            if (trade.mClose_time != 0) {
                // This is a closed trade being modified for some reason.  Just ignore it and don't output an error message.
            }
            else {
                logger.warn("Updating trade never seen before.  TradeObject was: {}", trade.toString());
            }
        }
    }

    private static final double sExposureImpactEpsilon = 1.0e-5;

    private static boolean isTradeExposureDifferent(final List<TradeExposureOnStrategy> oldTradeExposures, final List<TradeExposureOnStrategy> newTradeExposures) {

        final Map<Integer, TradeExposureOnStrategy> strategyToNewTradeExposuresMap = newTradeExposures.stream().collect(Functionals.toHashMap(TradeExposureOnStrategy::getStrategyId, Function.identity(), newTradeExposures.size()));

        return oldTradeExposures.stream()
            .filter(oldTradeExposure -> {
                final TradeExposure tOld = oldTradeExposure.getTradeExposure();
                final TradeExposure tNew = strategyToNewTradeExposuresMap.get(oldTradeExposure.getStrategyId()).getTradeExposure();

                final boolean tradeExposureIsTheSame = tOld.mIsLongShort == tNew.mIsLongShort
                    && Math.abs(tNew.mq1 - tOld.mq1) < sExposureImpactEpsilon
                    && Math.abs(tNew.mq2 - tOld.mq2) < sExposureImpactEpsilon;

                return !tradeExposureIsTheSame;
            })
            .findAny()
            .isPresent();
    }

    private static List<TradeExposureOnStrategy> generateAdjustmentExposuresOnStrategies(final List<TradeExposureOnStrategy> oldExposuresOnStrategies, final List<TradeExposureOnStrategy> newExposuresOnStrategies) {

        final Map<Integer, TradeExposureOnStrategy> strategyToNewTradeExposureMap = newExposuresOnStrategies.stream().collect(Functionals.toHashMap(TradeExposureOnStrategy::getStrategyId, Function.identity()));

        return oldExposuresOnStrategies.stream()
            .flatMap(t -> {

                final TradeExposure oldExp = t.getTradeExposure();
                final TradeExposure newExp = strategyToNewTradeExposureMap.get(t.getStrategyId()).getTradeExposure();

                final double mq1Difference = newExp.mq1 - oldExp.mq1;
                final double mq2Difference = newExp.mq2 - oldExp.mq2;

                if (Double.isNaN(mq1Difference) || Double.isNaN(mq2Difference)) {
                    return Stream.empty();
                }
                else {
                    return Stream.of(TradeExposureOnStrategy.create(t.getStrategyId(), TradeExposure.create(newExp.mIsLongShort, mq1Difference, mq2Difference), t.getRiskGroupName(), t.isClientRiskGroup()));
                }
            })
            .collect(Functionals.toArrayList(newExposuresOnStrategies.size()));
    }

    private void processSymbolUpdated(final Symbol symbol) {
        mSymbolCache.update(mt4j.platformObjects.Symbol.create(symbol));
    }

    private void processSymbolTickOccured(final TickInfo tickInfo) {
        if (mTickInfoQueue != null) {
            mTickInfoQueue.add(mt4j.platformObjects.TickInformation.create(tickInfo));
        }
        else {
            final String errString = "A tick info queue was not set up even though symbols were requested. Throwing exception...";
            logger.error(errString);
            throw new RuntimeException(errString);
        }
    }

    private List<TradeExposureOnStrategy> createTradeExposureOnStrategies(final Trade trade, final List<StrategyAffectingRiskGroup> strategyAffectingRiskGroups) {

        final SymbolWithDetails symbolWithDetails = mSymbolCache.get(trade.mSymbol);

        return strategyAffectingRiskGroups.stream()
            .map(rg -> {
                final RiskGroupWithExpFactor riskGroupWithExposureFactor = rg.getRiskGroupWithExpFactor();
                final double riskGroupWeight = riskGroupWithExposureFactor.getExposureFactor();
                final int strategyId = rg.getStrategyId();

                final double weightOnStrategy = riskGroupWeight * mStrategyParametersMap.get(strategyId).getExecutionInfo().getStrategyWeight();

                final TradeExposure tradeExposureOnStrategy = TradeExposure.create(
                    trade.isBuy(),
                    trade.mVolume,
                    weightOnStrategy,
                    trade.mOpen_price,
                    symbolWithDetails.getSymbolDetails().getCcyConvFactor(),
                    symbolWithDetails.getSymbol().getCalculationSettings().getContractSize(),
                    trade.mSymbol,
                    trade.mOpen_time);
//we make trades on mt4 anymore, so they are all client trades
                return TradeExposureOnStrategy.create(strategyId, tradeExposureOnStrategy, riskGroupWithExposureFactor.getRiskGroup().getGroupName(), true);
            })
            .collect(Functionals.toArrayList(strategyAffectingRiskGroups.size()));
    }

    private double getContractSize(final String symbol) {
        return mSymbolCache.get(symbol).getSymbol().getCalculationSettings().getContractSize();
    }

    private MessageProcessor(
        final ServerFarm.ServerDetails serverDetails,
        final BlockingQueue<MessageModule.Mt4Message> mt4Queue,
        final PumpingManager pumpingManager,
        final BlockingQueue<ExposuresMessage> collectorOutgoingQueue,
        final Map<Integer, StrategyParameters> strategyParametersMap,
        final CountDownLatch countDownLatchForServers,
        final CountDownLatch countDownLatchForCaller,
        final UserCache userCache,
        final SymbolCache symbolCache,
        final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay,
        final Map<Integer, Map<String, String>> strategyLpToSymbolNameMapping,
        final Map<Integer, TradeExposureInfo> openTrades,
        final BlockingQueue<TickInformation> tickInfoQueue) {

        mServerDetails = serverDetails;
        mMt4Queue = mt4Queue;
        mPumpingManager = pumpingManager;
        mModelOutgoingQueue = collectorOutgoingQueue;
        mOpenTrades = openTrades;

        mStrategyParametersMap = strategyParametersMap;

        mCountDownLatchForServers = countDownLatchForServers;
        mCountDownLatchForCaller = countDownLatchForCaller;

        mTimeToDie = false;

        mProcessTradeOpened = this::processTradeOpened;
        mProcessTradeClosed = this::processTradeClosed;
        mProcessTradeDeletedByAdmin = this::processTradeDeletedByAdmin;
        mProcessTradeUpdate = this::processTradeUpdate;

        mUserCache = userCache;
        mSymbolCache = symbolCache;
        mStrategySymbolsInPlay = strategySymbolsInPlay;
        mParentSymbolsCache = new HashMap<>();

        mUserInfoBiFunction = (server, userId) -> mUserCache.get(AnalystMt4PumpingManager.create(getPumpingManager()), userId);

        mStrategyLpToSymbolNameMapping = strategyLpToSymbolNameMapping;
        mTickInfoQueue = tickInfoQueue;
    }

    private final ServerFarm.ServerDetails mServerDetails;

    private final PumpingManager mPumpingManager;
    private final BlockingQueue<MessageModule.Mt4Message> mMt4Queue;
    private final BlockingQueue<ExposuresMessage> mModelOutgoingQueue;

    private final Map<Integer, TradeExposureInfo> mOpenTrades;

    private final Map<Integer, StrategyParameters> mStrategyParametersMap;

    private final BlockingQueue<TickInformation> mTickInfoQueue;
    private final CountDownLatch mCountDownLatchForServers;
    private final CountDownLatch mCountDownLatchForCaller;

    private volatile boolean mTimeToDie;

    private final BooleanIntObjObjConsumer<Trade, List<StrategyAffectingRiskGroup>> mProcessTradeOpened;
    private final BooleanIntObjObjConsumer<Trade, List<StrategyAffectingRiskGroup>> mProcessTradeClosed;
    private final BooleanIntObjObjConsumer<Trade, List<StrategyAffectingRiskGroup>> mProcessTradeDeletedByAdmin;
    private final BooleanIntObjObjConsumer<Trade, List<StrategyAffectingRiskGroup>> mProcessTradeUpdate;

    private final UserCache mUserCache;
    private final SymbolCache mSymbolCache;
    private final Map<Integer, Map<String, SymbolMetaData>> mStrategySymbolsInPlay;
    private final Map<String, String> mParentSymbolsCache;

    private final BiFunction<Integer, Long, UserInfo> mUserInfoBiFunction;

    private final Map<Integer, Map<String, String>> mStrategyLpToSymbolNameMapping;
}
