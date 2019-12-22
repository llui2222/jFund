package com.xm.jfund.exposures;

import com.xm.jfund.client.trade.TradeServiceRestClient;
import com.xm.jfund.client.trade.builder.TradeServiceWebSocketClientBuilder;
import com.xm.jfund.client.trade.exception.TradeServiceException;
import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.client.trade.model.Trade;
import com.xm.jfund.exposures.handler.TradeServiceWebSocketHandler;
import com.xm.jfund.queuemessages.ExposuresMessage;
import com.xm.jfund.queuemessages.ExposuresMessageFactory;
import com.xm.jfund.queuemessages.TradeServiceConnectionEstablished;
import com.xm.jfund.queuemessages.TradeServiceError;
import com.xm.jfund.queuemessages.TradeServiceMessage;
import com.xm.jfund.queuemessages.TradeServicePosition;
import com.xm.jfund.queuemessages.TradeServiceVisitor;
import com.xm.jfund.utils.StrategyExecutionInfo;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.StrategySymbolLimits;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import jAnalystUtils.SymbolModule;
import jAnalystUtils.SymbolModule.SymbolMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TradeServiceMessageProcessor implements TradeServiceVisitor, IMessageProcessor {

    private final Logger logger = LoggerFactory.getLogger(TradeServiceMessageProcessor.class);

    private static final int POLLING_TIMEOUT = 800;

    private final String socketUrl;
    private final String positionUpdatesQueue;
    private final TradeServiceRestClient tradeServiceRestClient;
    private final Map<Integer, StrategyParameters> strategyParameters;
    private final CountDownLatch countDownLatchForServers;
    private final CountDownLatch countDownLatchForCaller;
    private final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay;
    private final BlockingDeque<ExposuresMessage> exposuresOutgoingQueue;
    private final BlockingQueue<TradeServiceMessage> processorQueue;
    private final WebSocketStompClient webSocketClient;
    private final TradeServiceWebSocketHandler webSocketHandler;
    private final Map<String, Map<String, Long>> takerNameTakerLoginToSymbolToTradeSequenceNumber;
    private volatile boolean timeToDie;
    private volatile StompSession session;

    private TradeServiceMessageProcessor(final String username,
                                         final String password,
                                         final String socketUrl,
                                         final String positionUpdatesQueue,
                                         final TradeServiceRestClient tradeServiceRestClient,
                                         final Map<Integer, StrategyParameters> strategyParameters,
                                         final CountDownLatch countDownLatchForServers,
                                         final CountDownLatch countDownLatchForCaller,
                                         final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay,
                                         final BlockingDeque<ExposuresMessage> exposuresOutgoingQueue,
                                         final Map<String, Map<String, Long>> takerNameTakerLoginToSymbolToTradeSequenceNumber) {
        this.socketUrl = socketUrl;
        this.positionUpdatesQueue = positionUpdatesQueue;
        this.tradeServiceRestClient = tradeServiceRestClient;
        this.strategyParameters = strategyParameters;
        this.countDownLatchForServers = countDownLatchForServers;
        this.countDownLatchForCaller = countDownLatchForCaller;
        this.strategySymbolsInPlay = strategySymbolsInPlay;
        this.exposuresOutgoingQueue = exposuresOutgoingQueue;
        this.processorQueue = new LinkedBlockingQueue<>();
        this.webSocketHandler = TradeServiceWebSocketHandler.create(processorQueue);
        this.webSocketClient = TradeServiceWebSocketClientBuilder.create(username, password).build();
        this.takerNameTakerLoginToSymbolToTradeSequenceNumber = takerNameTakerLoginToSymbolToTradeSequenceNumber;
    }

    public static TradeServiceMessageProcessor create(final String username,
                                                      final String password,
                                                      final String socketUrl,
                                                      final String positionUpdatesQueue,
                                                      final TradeServiceRestClient tradeServiceRestClient,
                                                      final Map<Integer, StrategyParameters> strategyParameters,
                                                      final CountDownLatch countDownLatchForServers,
                                                      final CountDownLatch countDownLatchForCaller,
                                                      final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay,
                                                      final BlockingDeque<ExposuresMessage> exposuresOutgoingQueue) {

        final Map<String, Map<String, Long>> takerNameTakerLoginToSymbolToTradeSequenceNumber = createSequenceNumberMap(strategyParameters);

        return new TradeServiceMessageProcessor(
            username,
            password,
            socketUrl,
            positionUpdatesQueue,
            tradeServiceRestClient,
            strategyParameters,
            countDownLatchForServers,
            countDownLatchForCaller,
            strategySymbolsInPlay,
            exposuresOutgoingQueue,
            takerNameTakerLoginToSymbolToTradeSequenceNumber);
    }

    static Map<String, Map<String, Long>> createSequenceNumberMap(final Map<Integer, StrategyParameters> strategyParameters) {
        final Map<String, Map<String, Long>> takerNameTakerLoginToSymbolToSequenceNumberMap = new HashMap<>();
        for (final StrategyParameters parameters : strategyParameters.values()) {
            final Map<String, Map<TYPE, StrategySymbolLimits>> symbolLimits = parameters.getSymbolLimits();
            final StrategyExecutionInfo executionInfo = parameters.getExecutionInfo();
            final String takerLogin = executionInfo.getTakerLogin();
            final String takerName = executionInfo.getTakerName();

            //we populate zero since if there are no exposures existing, the first sequence number will be 1
            final Map<String, Long> symbolToSequenceNumber = symbolLimits.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> 0L));
            takerNameTakerLoginToSymbolToSequenceNumberMap.put(makeTakerNameTakerLoginKey(takerName, takerLogin), symbolToSequenceNumber);
        }
        return takerNameTakerLoginToSymbolToSequenceNumberMap;
    }

    @Override
    public void run() {
        try {
            while (!timeToDie) {
                final Optional<TradeServiceMessage> msgOpt = getNextMessage();
                msgOpt.ifPresent(msg -> msg.accept(this));
            }
        }
        finally {
            countDownLatchForServers.countDown();
            countDownLatchForCaller.countDown();
            logger.info("Message processor for trade service has ended.");
        }
    }

    private Optional<TradeServiceMessage> getNextMessage() {
        try {
            return Optional.ofNullable(processorQueue.poll(POLLING_TIMEOUT, TimeUnit.MILLISECONDS));
        }
        catch (final InterruptedException ex) {
            logger.error("The thread was interrupted while polling for messages.", ex);
            throw new RuntimeException("The thread was interrupted while polling for messages.", ex);
        }
    }

    @Override
    public void visit(final TradeServiceConnectionEstablished msg) {
        final StompSession session = msg.getSession();
        logger.info("Successfully connected to trade service - session id {}.", session.getSessionId());

        countDownLatchForServers.countDown();
        try {
            countDownLatchForServers.await();
        }
        catch (final InterruptedException ex) {
            logger.error("The thread was interrupted while waiting on the countdown latch.", ex);
            throw new RuntimeException(ex);
        }
        processExposures();

        this.countDownLatchForCaller.countDown();
    }

    @Override
    public void visit(final TradeServicePosition msg) {
        onTradeReceived(msg.getTrade());
    }

    @Override
    public void visit(final TradeServiceError msg) {
        final Throwable error = msg.getError();
        logger.error("The connection to trade service has been lost.", error);
        if (error instanceof ConnectionLostException) {
            throw new MessageProcessorConnectionLostException("Connection to trade service lost.  Will attempt to reconnect...");
        }
        else {
            throw new RuntimeException("Trade Service Message Process error.", error);
        }
    }

    @Override
    public boolean connect() {
        try {
            this.session = webSocketClient.connect(socketUrl, webSocketHandler).get();
            session.subscribe(positionUpdatesQueue, webSocketHandler);
        }
        catch (final Exception e) {
            disconnect();
            logger.warn("Error while connecting to trade service: {}", e.getMessage());
        }
        return isConnected();
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            session.disconnect();
        }
        processorQueue.clear();
    }

    @Override
    public void timeToDie() {
        this.timeToDie = true;
    }

    @Override
    public void cleanup() {
        disconnect();
    }

    @Override
    public String getServerName() {
        return "trade-service";
    }

    private boolean isConnected() {
        return session != null && session.isConnected();
    }

    private void onTradeReceived(final Trade trade) {

        logger.info("Received trade  {}", trade);

        final List<Integer> matchingStrategies = strategyParameters.entrySet().stream()
            .filter(entry -> symbolIsTracked(trade.getSymbol(), entry.getKey()))
            .filter(entry -> entry.getValue().getTradingAccountRiskGroup().isMember(trade.getTakerName(), trade.getTakerLogin()))
            .map(Entry::getKey)
            .collect(Collectors.toList());

        if (!matchingStrategies.isEmpty()) {
            final long tradeSequenceNumber = trade.getTradeSequenceNumber();
            final String symbol = trade.getSymbol();
            final String takerName = trade.getTakerName();
            final String takerLogin = trade.getTakerLogin();
            final Map<String, Long> symbolToTradeSequenceNumber = takerNameTakerLoginToSymbolToTradeSequenceNumber.get(makeTakerNameTakerLoginKey(takerName, takerLogin));
            final long initialTradeSequenceNumber = symbolToTradeSequenceNumber.get(symbol);

            if (tradeSequenceNumber > initialTradeSequenceNumber) {

                exposuresOutgoingQueue.addFirst(ExposuresMessageFactory.create(matchingStrategies, trade));
            }
            else {
                logger.warn("Discarding already accounted for trade. Trade sequence number: {}, initial trade sequence number: {}, Trade: {}", tradeSequenceNumber, initialTradeSequenceNumber, trade);
            }
        }
    }

    private boolean symbolIsTracked(final String tradeSymbol, final Integer strategyId) {

        if (strategySymbolsInPlay.get(strategyId).containsKey(tradeSymbol)) {
            return true;
        }
        else {
            final Optional<SymbolMetaData> symbolMetaDataOpt = SymbolModule.getSymbolMetaDataByName(tradeSymbol);

            return symbolMetaDataOpt.isPresent() && strategySymbolsInPlay.get(strategyId).containsKey(symbolMetaDataOpt.get().getParentSymbol());
        }
    }

    private void processExposures() {

        for (final Entry<Integer, StrategyParameters> entry : strategyParameters.entrySet()) {
            final int strategyId = entry.getKey();
            final StrategyExecutionInfo info = entry.getValue().getExecutionInfo();
            try {
                final String takerLogin = info.getTakerLogin();
                final String takerName = info.getTakerName();

                final List<Exposure> exposures = getExposuresForProperSymbols(takerName, strategyId, takerLogin);

                populateTradeSequenceMap(takerName, exposures, takerLogin, takerNameTakerLoginToSymbolToTradeSequenceNumber);

                logger.info("Trade sequence numbers populated. Sequence map: {}", takerNameTakerLoginToSymbolToTradeSequenceNumber);

                final List<ExposuresMessage> exposuresMessages = getExposureMessages(strategyId, exposures);

                exposuresMessages.forEach(exposuresOutgoingQueue::addFirst);
            }
            catch (final TradeServiceException e) {
                throw new RuntimeException(String.format("Error while retrieving exposures for strategy %s", strategyId));
            }
        }
    }

    private List<ExposuresMessage> getExposureMessages(final int strategyId, final List<Exposure> exposures) {
        return exposures.stream().map(exp -> ExposuresMessageFactory.create(strategyId, exp)).collect(Collectors.toList());
    }

    static String makeTakerNameTakerLoginKey(final String takerName, final String takerLogin) {
        return takerName + ":" + takerLogin;
    }

    static void populateTradeSequenceMap(final String takerName, final List<Exposure> exposures, final String takerLogin, final Map<String, Map<String, Long>> sequenceNumberMap) {
        for (final Exposure exposure : exposures) {
            final String symbol = exposure.getSymbol();
            final Map<String, Long> symbolToTradeSequenceNumber = sequenceNumberMap.get(makeTakerNameTakerLoginKey(takerName, takerLogin));

            final Long tradeSequenceNumber = exposure.getTradeSequenceNumber();
            if (tradeSequenceNumber == null || tradeSequenceNumber <= 0) {
                throw new RuntimeException("Trade sequence number is not correct. Number: " + tradeSequenceNumber);
            }

            symbolToTradeSequenceNumber.put(symbol, exposure.getTradeSequenceNumber());
        }
    }

    private List<Exposure> getExposuresForProperSymbols(final String takerName, final int strategyId, final String takerLogin) throws TradeServiceException {
        return tradeServiceRestClient.getExposures(takerName, takerLogin).stream()
            .filter(exp -> symbolIsTracked(exp.getSymbol(), strategyId)).collect(Collectors.toList());
    }
}
