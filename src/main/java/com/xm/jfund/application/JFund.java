package com.xm.jfund.application;

import com.xm.jfund.application.threadfactories.JFundThreadFactory;
import com.xm.jfund.client.trade.TradeServiceRestClient;
import com.xm.jfund.client.trade.builder.TradeServiceRestClientBuilder;
import com.xm.jfund.controllers.DecisionController;
import com.xm.jfund.controllers.DecisionControllerFactory;
import com.xm.jfund.db.JFundDBConnectionProvider;
import com.xm.jfund.execution.TradeExecutor;
import com.xm.jfund.exposures.ExposuresCollector;
import com.xm.jfund.exposures.IMessageProcessor;
import com.xm.jfund.exposures.MessageProcessor;
import com.xm.jfund.exposures.MessageProcessorConnectionLostException;
import com.xm.jfund.exposures.TradeServiceMessageProcessor;
import com.xm.jfund.monitoring.ExposureSender;
import com.xm.jfund.monitoring.ZabbixHandler;
import com.xm.jfund.queuemessages.ExposuresMessage;
import com.xm.jfund.queuemessages.TradeInfoWithStrategyExposures;
import com.xm.jfund.riskgroup.RiskGroupWithExpFactor;
import com.xm.jfund.riskgroup.StrategyRiskGroupLoader;
import com.xm.jfund.riskgroup.TradingAccountRiskGroup;
import com.xm.jfund.utils.ConnectionUtils;
import com.xm.jfund.utils.CurrentActiveThresholdTypeAuditHandler;
import com.xm.jfund.utils.DataLoader;
import com.xm.jfund.utils.EnvUtils;
import com.xm.jfund.utils.KeyGenerator;
import com.xm.jfund.utils.LogExposuresMode;
import com.xm.jfund.utils.NotificationUtils;
import com.xm.jfund.utils.NotificationUtils.NotificationLevel;
import com.xm.jfund.utils.StrategyExecutionInfo;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.StrategySymbolLimits;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import com.xm.jfund.utils.ThreadUtils;
import com.xm.jfund.utils.TradeExposureInfo;
import com.xm.jfund.utils.TradeForExecution;
import com.xm.jfund.utils.ValidationUtils;
import com.xm.jfund.zabbixobjects.DiscoveryMessage;
import com.xm.jfund.zabbixobjects.ZabbixMessage;
import jAnalystUtils.SymbolModule;
import jAnalystUtils.SymbolModule.SymbolCache;
import jAnalystUtils.SymbolModule.SymbolMetaData;
import jAnalystUtils.SymbolModule.SymbolWithDetails;
import jAnalystUtils.UserCache;
import jAnalystUtils.currencyConversion.CurrencyConversionPump;
import jAnalystUtils.currencyConversion.TickInfoInternal;
import jManagerUtils.JManagerServerEnvironment;
import jManagerUtils.ManagerCommandResponseModule.FetchRatesResponse;
import jManagerUtils.SymbolRateWithDigits;
import jManagerUtils.jManagerServerApi.JManagerServerApiModule;
import jManagerUtils.jManagerServerApi.WebServiceError;
import jxmUtils.BuildMode;
import jxmUtils.CommandVersion;
import jxmUtils.Either;
import jxmUtils.EmailSender;
import jxmUtils.EmailSender.SendMode;
import jxmUtils.Functionals;
import jxmUtils.HttpProtocol;
import jxmUtils.HttpProtocolKind;
import jxmUtils.ServerFarm.PlatformType;
import jxmUtils.ServerFarm.ServerDetails;
import jxmUtils.ServerFarm.ServerType;
import jxmUtils.SymbolRate;
import jxmUtils.SystemProperties;
import jxmUtils.TupleModule.Pair;
import jxmUtils.webServicesModule.WebServiceCallConfig;
import mt4j.PumpingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import platformObjects.interfaces.TickInformation;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author nmichael
 */

public final class JFund {

    private static final Logger logger = LoggerFactory.getLogger(JFund.class);

    private static final Class<JFund> sc = JFund.class;

    private static final TradeInfoWithStrategyExposures sSentinelTradeInfoWithStrategyExposures = TradeInfoWithStrategyExposures.create(Collections.emptyList(), null, -1);
    private static final int sManagerLogin = 391;
    private static final String sManagerPassword = "fgt4wpmzn!!";

    private static final int sFundXmUserId = 10020;
    private static final int sServerIndexForConversions = 0;

    private static Map<Integer, StrategyParameters> getStrategyImmutableParameters(final Map<Integer, StrategyExecutionInfo> strategyExecutionInfoMap, final Map<Integer, List<RiskGroupWithExpFactor>> strategyToRiskGroupsWithExposureFactorsMap) throws SQLException {

        final Map<Integer, Double> strategyToCommonClientGroupWeightMap = strategyToRiskGroupsWithExposureFactorsMap.entrySet().stream()
            .map(entry -> {
                final Integer strategyId = entry.getKey();
                final double commonGroupWeight = entry.getValue().get(0).getExposureFactor();
                return Pair.create(strategyId, commonGroupWeight);
            })
            .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));

        final Map<Integer, Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>>> strategySymbolThresholds = DataLoader.loadStrategySymbolThresholds(strategyExecutionInfoMap.keySet());
        final Map<Integer, Map<String, String>> strategyToSymbolToLpNameConversionMap = DataLoader.getStrategyToLPSymbolMap(strategyExecutionInfoMap);

        return strategyToRiskGroupsWithExposureFactorsMap.entrySet().stream()
            .map(entry -> {
                final Integer strategyId = entry.getKey();
                final StrategyExecutionInfo executionInfo = strategyExecutionInfoMap.get(strategyId);
                final List<RiskGroupWithExpFactor> trackedRiskGroups = entry.getValue();
                final double commonClientGroupWeight = strategyToCommonClientGroupWeightMap.get(strategyId);
                final Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>> symbolLimitsMap = strategySymbolThresholds.get(strategyId);
                final Map<String, String> symbolToLpNameConversionMap = strategyToSymbolToLpNameConversionMap.get(strategyId);
                final TradingAccountRiskGroup tradingAccountRiskGroup = StrategyRiskGroupLoader.getTradingAccountRiskGroup(strategyId, executionInfo.getTakerName(), executionInfo.getTakerLogin());

                return Pair.create(strategyId, StrategyParameters.create(executionInfo, trackedRiskGroups, commonClientGroupWeight, symbolLimitsMap, symbolToLpNameConversionMap, tradingAccountRiskGroup));
            })
            .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));
    }

    private static Map<Integer, ServerDetails> getServerDetails(final JManagerServerApiModule jManagerServerApiModule, final Set<Integer> removedServers, final BuildMode buildMode) {

        final List<ServerType> requestServerTypes = buildMode.isRelease() ? Collections.singletonList(ServerType.LIVE) : Arrays.asList(ServerType.LIVE, ServerType.MIRROR);

        return jManagerServerApiModule.fetchServerFarm(requestServerTypes, sFundXmUserId, SystemProperties.getMachineIP(), CommandVersion.V1, WebServiceCallConfig.sDefaultWebServiceCallConfig)
            .getRight().getServerFarm().values().stream()
            .flatMap(List::stream)
            .filter(sd -> sd.getPlatformType() == PlatformType.MT4)
            .filter(sd -> sd.getServerType() == ServerType.LIVE)
            .filter(sd -> !removedServers.contains(sd.getServerId()))
            .collect(Functionals.toHashMap(ServerDetails::getServerId, Function.identity()));
    }

    /**
     * Initialize email for issue reporting
     *
     * @param buildMode        build mode of jfund
     * @param parametersLoader initialization parameters for jfund
     */
    private static void initEmail(final BuildMode buildMode, final ParametersLoader parametersLoader) {
        final SendMode notificationMode = parametersLoader.getNotificationMode();

        EmailSender.init(notificationMode, buildMode);
    }

    /**
     * Validate some strategy parameters as well as collect all strategy related info
     *
     * @param parametersLoader   jfund parameters loader
     * @param disabledStrategies strategies that are disabled
     * @return map of strategy id to strategy parameters
     * @throws SQLException if a db related issue occurs
     */
    private static Map<Integer, StrategyParameters> handleRetrievingStrategyData(final ParametersLoader parametersLoader, final Set<Integer> disabledStrategies) throws SQLException {
        final Map<Integer, StrategyExecutionInfo> strategyToExecutionInfoMap = parametersLoader.getExecutionInfo();

        final Map<Integer, List<RiskGroupWithExpFactor>> strategyToRiskGroupsWithExposureFactorsMap = StrategyRiskGroupLoader.loadRiskGroupsWithExposureFactor(strategyToExecutionInfoMap);

        StrategyRiskGroupLoader.ensureClientRiskGroupsHaveCommonWeight(strategyToRiskGroupsWithExposureFactorsMap);
        ValidationUtils.ensureExecutionArgumentsAreLoaded(strategyToExecutionInfoMap, parametersLoader.getStrategies());
        NotificationUtils.printRunInformation(strategyToExecutionInfoMap, strategyToRiskGroupsWithExposureFactorsMap, disabledStrategies, sc);

        return getStrategyImmutableParameters(strategyToExecutionInfoMap, strategyToRiskGroupsWithExposureFactorsMap);
    }

    public static void main(final String[] args) {
        try {
            final String activeProfile = EnvUtils.getActiveProfile();
            logger.info("Staring up JFund with profile: {}", activeProfile);
            final ParametersLoader parametersLoader = ParametersLoader.create(EnvUtils.getProfileConfigurationFileName(activeProfile));

            JFundDBConnectionProvider.init(parametersLoader.getJFundDB());
            logger.info("JFund is finished loading parameters.");

            final BuildMode buildMode = parametersLoader.getBuildMode();

            final boolean isProduction = buildMode == BuildMode.RELEASE;

            initEmail(buildMode, parametersLoader);

            final Set<Integer> disabledStrategies = DataLoader.loadDisabledStrategies();
            logger.info("JFund is loading database specific parameters...");
            final Map<Integer, StrategyParameters> strategyParametersMap = handleRetrievingStrategyData(parametersLoader, disabledStrategies);
            logger.info("JFund is finished loading database specific parameters...");

            final LogExposuresMode logExposuresMode = parametersLoader.getLogExposuresMode();

            final List<Integer> removedServersList = parametersLoader.getRemovedServers();
            final Set<Integer> removedServers = removedServersList.stream().collect(Functionals.toHashSet(removedServersList.size()));

            final SymbolCache symbolCache = SymbolCache.create();
            final UserCache userCache = UserCache.create();

            final JManagerServerApiModule jManagerServerApiModule = JManagerServerApiModule.create(
                buildMode.isRelease()
                    ? JManagerServerEnvironment.LIVE
                    : JManagerServerEnvironment.TEST,
                HttpProtocol.create(HttpProtocolKind.HTTPS, Optional.of(8443)));

            final DecisionController decisionController = DecisionControllerFactory.create(parametersLoader.getDecisionControllerType());
            final String zabbixHostName = parametersLoader.getZabbixHostName();
            final int zabbixPort = parametersLoader.getZabbixPort();
            final String zabbixProjectHost = parametersLoader.getZabbixProjectHost();

            final TradeServiceRestClient tradeServiceRestClient = TradeServiceRestClientBuilder.create(parametersLoader.getTradeServiceUsername(), parametersLoader.getTradeServicePassword(), parametersLoader.getTradeServiceBaseUrl()).build();

            while (true) {

                try {
                    logger.info("JFund is retrieving server details...");
                    final Map<Integer, ServerDetails> serverIdToDetailsMap = getServerDetails(jManagerServerApiModule, removedServers, buildMode);
                    logger.info("JFund is finished retrieving server details.");
                    /* The values of this map (the internal TradeId to Exposure Maps) are given to the MessageProcessors to update,
                     * The state is saved on this thread so it can be passed anew to the new MessageProcessors in case of a restart.
                     * The maps are ConcurrentHashMaps because ExposuresCollector also reads from it.
                     */
                    final Map<Integer, ConcurrentMap<Integer, TradeExposureInfo>> serverToOpenTradeToExposureMap = new ConcurrentHashMap<>();
                    serverIdToDetailsMap.keySet().forEach(serverId -> serverToOpenTradeToExposureMap.put(serverId, new ConcurrentHashMap<>()));

                    //Also count trade service
                    final int numServers = serverIdToDetailsMap.size() + 1;

                    final List<CompletableFuture<Void>> tasksList = new ArrayList<>();
                    final BlockingQueue<ZabbixMessage> zabbixMessageQueue = new LinkedBlockingQueue<>();

                    final Map<String, StrategySymbolLimits> activeStrategySymbolLimitsPerStrategyAndSymbol =
                        initiateCurrentActiveStrategySymbolLimitsPerStrategyAndSymbol(strategyParametersMap);

                    final ZabbixMessage discoveryMessage = DiscoveryMessage.create(parametersLoader.getZabbixDiscoveryTrapper(), new ArrayList<>(activeStrategySymbolLimitsPerStrategyAndSymbol.values()));

                    final ZabbixHandler zabbixHandler = ZabbixHandler.create(zabbixMessageQueue, discoveryMessage, zabbixHostName, zabbixPort, zabbixProjectHost);
                    final ExecutorService zabbixExecutorService = Executors.newSingleThreadExecutor(JFundThreadFactory.create("ZabbixHandler", new ArrayList<>()));
                    tasksList.add(CompletableFuture.runAsync(zabbixHandler, zabbixExecutorService));

                    logger.info("JFund is loading symbol data...");

                    DataLoader.loadSymbolMetaData(buildMode);

                    logger.info("JFund is finished loading symbol data.");

                    logger.info(ManagementFactory.getRuntimeMXBean().getName());

                    final int messageRequest = getMessageRequest();
                    final BlockingDeque<ExposuresMessage> exposureCollectorQueue = new LinkedBlockingDeque<>();
                    final BlockingQueue<TradeForExecution> tradeBlockingQueue = new LinkedBlockingQueue<>();

                    final Map<Integer, BlockingQueue<byte[]>> strategyToExposureSenderQueueMap = strategyParametersMap.keySet().stream()
                        .map(s -> Pair.create(s, new ArrayBlockingQueue<byte[]>(1)))
                        .collect(Functionals.toConcurrentHashMap(Pair::getFirst, Pair::getSecond));

                    final CountDownLatch countDownLatchForServers = new CountDownLatch(numServers);
                    final CountDownLatch countDownLatchForCaller = new CountDownLatch(numServers);

                    symbolCache.resetCache(); // If there was a server restart then reload the symbolCache -- perhaps symbol definitions have changed.

                    final TradeExecutor tradeExecutor = TradeExecutor.create(
                        tradeBlockingQueue,
                        strategyParametersMap,
                        exposureCollectorQueue,
                        zabbixMessageQueue,
                        parametersLoader.getZabbixTradesTrapperTemplate(),
                        tradeServiceRestClient,
                        disabledStrategies);

                    final ExecutorService tradeExecutorExecutorService = Executors.newSingleThreadExecutor(JFundThreadFactory.create("TradeExecutor", new ArrayList<>()));
                    final CompletableFuture<Void> tradeExecutorFuture = CompletableFuture.runAsync(tradeExecutor, tradeExecutorExecutorService);
                    tasksList.add(tradeExecutorFuture);

                    final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay = DataLoader.loadSymbolsOfInterest(strategyParametersMap);

                    ValidationUtils.ensureSymbolThresholdsWereLoaded(strategySymbolsInPlay, strategyParametersMap.keySet());

                    final Map<Integer, Map<String, String>> strategyToLpNameToNormalNameMap = DataLoader.getReverseLpSymbolMappings(strategyParametersMap);
                    final BlockingQueue<TickInformation> tickQueue = new LinkedBlockingQueue<>();
                    final List<IMessageProcessor> processors
                        = Functionals.zipWithIndex(
                        serverIdToDetailsMap.values().stream(),
                        (idx, serverDetails) ->
                            MessageProcessor.create(
                                serverDetails,
                                exposureCollectorQueue,
                                sManagerLogin,
                                sManagerPassword,
                                messageRequest | (idx == sServerIndexForConversions ? PumpingManager.sRequestSymbolTickOccured : 0),
                                strategyParametersMap,
                                countDownLatchForServers,
                                countDownLatchForCaller,
                                userCache,
                                symbolCache,
                                strategySymbolsInPlay,
                                strategyToLpNameToNormalNameMap,
                                serverToOpenTradeToExposureMap.get(serverDetails.getServerId()),
                                (idx == sServerIndexForConversions ? tickQueue : null))).collect(Functionals.toArrayList(serverIdToDetailsMap.size()));

                    processors.add(TradeServiceMessageProcessor.create(
                        parametersLoader.getTradeServiceUsername(),
                        parametersLoader.getTradeServicePassword(),
                        parametersLoader.getTradeServiceSocketUrl(),
                        "/topic/position-updates",
                        tradeServiceRestClient,
                        strategyParametersMap,
                        countDownLatchForServers,
                        countDownLatchForCaller,
                        strategySymbolsInPlay,
                        exposureCollectorQueue));

                    // Does not return unless it succeeds.
                    ConnectionUtils.connectProcessorsToServers(processors);

                    final ExecutorService messageProcessorExecutorService = Executors.newFixedThreadPool(numServers, JFundThreadFactory.create("MessageProcessor", IntStream.rangeClosed(1, numServers).boxed().collect((Collectors.toList()))));
                    for (final IMessageProcessor processor : processors) {
                        tasksList.add(CompletableFuture.runAsync(processor, messageProcessorExecutorService));
                    }

                    logger.info("Waiting for connection established latch...");
                    countDownLatchForCaller.await();
                    logger.info("Latch count has reached zero.");

                    // Notify the collector via this sentinel trade that it is time to start trading activity.
                    // This trade will appear in its queue after all existing open trades.
                    exposureCollectorQueue.add(sSentinelTradeInfoWithStrategyExposures);

                    final List<Pair<SymbolWithDetails, Double>> symbolsForConversion =
                        symbolCache.getSymbolsOfType(SymbolModule.sALL_SYMBOL_KINDS).stream()
                            .filter(s -> s.getSymbolDetails().getParentSymbol() == null)
                            .filter(s -> !s.getSymbolDetails().isTestSymbol())
                            .map(s -> Pair.create(s, s.getSymbolDetails().getCcyConvFactor()))
                            .collect(Functionals.toArrayList());

                    final List<String> symbolsToRegister = new ArrayList<>(symbolsForConversion.stream().map(p -> p.getFirst().getSymbol().getSymbol()).collect(Functionals.toHashSet()));

                    final List<String> assets = symbolCache.getAllAssets();

                    final String dstCurrency = "EUR";

                    final Map<Integer, Set<SymbolMetaData>> strategyTrackedSymbolsMetaData = strategySymbolsInPlay.entrySet().stream()
                        .map(e -> Pair.create(e.getKey(), e.getValue().values().stream().collect(Functionals.toHashSet())))
                        .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));

                    logger.info("Starting symbol subscription...");
                    final Map<String, TickInfoInternal> initialQuotes = subscribeToSymbolsAndRetrieveTicks(jManagerServerApiModule, (MessageProcessor) processors.get(sServerIndexForConversions), symbolsToRegister);
                    logger.info("Symbol subscription ended.");

                    final Pair<List<SymbolWithDetails>, CurrencyConversionPump> ccp = CurrencyConversionPump.create(
                        symbolsForConversion,
                        assets,
                        dstCurrency,
                        initialQuotes,
                        tickQueue);

                    final CurrencyConversionPump conversionPump = ccp.getSecond();
                    final ExposuresCollector expoCollector = ExposuresCollector.create(
                        decisionController,
                        strategyParametersMap,
                        exposureCollectorQueue,
                        conversionPump,
                        symbolCache,
                        tradeBlockingQueue,
                        sSentinelTradeInfoWithStrategyExposures,
                        logExposuresMode,
                        zabbixMessageQueue,
                        parametersLoader.getZabbixExposuresTrapperTemplate(),
                        parametersLoader.getZabbixRequestsTrapperTemplate(),
                        strategyToExposureSenderQueueMap,
                        strategyTrackedSymbolsMetaData,
                        disabledStrategies,
                        isProduction);
                    final ExecutorService conversionPumpExecutorService = Executors.newSingleThreadExecutor(JFundThreadFactory.create("CurrencyConversion", new ArrayList<>()));

                    tasksList.add(CompletableFuture.runAsync(conversionPump, conversionPumpExecutorService));

                    final List<Integer> strategies = new ArrayList<>(strategyToExposureSenderQueueMap.keySet());

                    final ExecutorService exposureSenderExecutorService = Executors.newFixedThreadPool(strategies.size(), JFundThreadFactory.create("ExposureSender_for_strategy", strategies));
                    final List<ExposureSender> exposureSenders = createExposureSenders(strategies, strategyToExposureSenderQueueMap);

                    for (final ExposureSender exposureSender : exposureSenders) {
                        tasksList.add(CompletableFuture.runAsync(exposureSender, exposureSenderExecutorService));
                    }

                    final ExecutorService exposureCollectorsExecutorService = Executors.newSingleThreadExecutor(JFundThreadFactory.create("ExposureCollector", new ArrayList<>()));
                    tasksList.add(CompletableFuture.runAsync(expoCollector, exposureCollectorsExecutorService));

                    logger.info("All tasks have now been started...");
                    final long timeToWaitInMillisForThreadPrinting = 20_000;

                    //should be handleasync, if we don't, the completed future thread will try to shut it's own executor down, and hang forever
                    final CompletableFuture<Object> allTasksFuture = CompletableFuture.anyOf(tasksList.toArray(new CompletableFuture<?>[tasksList.size()])).handleAsync(
                        getShutDownHandler(buildMode,
                            strategyParametersMap.keySet(),
                            zabbixExecutorService,
                            exposureCollectorsExecutorService,
                            exposureSenderExecutorService,
                            tradeExecutorExecutorService,
                            messageProcessorExecutorService,
                            conversionPumpExecutorService,
                            processors,
                            exposureSenders,
                            expoCollector,
                            tradeExecutor,
                            conversionPump,
                            zabbixHandler,
                            timeToWaitInMillisForThreadPrinting));
                    allTasksFuture.get();
                }
                catch (final Throwable ex) {
                    logger.error(ex.getMessage(), ex);
                    if (isProduction) {
                        NotificationUtils.notifyAdmins(NotificationLevel.EMAIL_ONLY, "jFund has abruptly stopped please investigate. It will be restarted now.", ex);
                    }
                }

                Thread.sleep(sWaitTimeToRestartInMilliseconds);
            }
        }
        catch (final Exception ex) {
            logger.error("Failed to start jFund: ", ex);
            EmailSender.shutdown();
        }
    }

    /**
     * Get shutdown handler for when jfund runs into an unexpected exception or it completes
     * unexpectedly.
     *
     * @param buildMode                           build mode
     * @param zabbixExecutorService               executor service for zabbix handler
     * @param exposureCollectorsExecutorService   executor service for exposures collector
     * @param exposureSenderExecutorService       executor service for exposure sender
     * @param tradeExecutorExecutorService        executor service for trade executor
     * @param messageProcessorExecutorService     executor service for message processors
     * @param conversionPumpExecutorService       executor service for conversion pump
     * @param processors                          list of processors
     * @param exposureSenders                     list of exposure senders
     * @param expoCollector                       exposures collector
     * @param tradeExecutor                       trade executor
     * @param conversionPump                      conversion pump
     * @param zabbixHandler                       zabbix handler
     * @param timeToWaitInMillisForThreadPrinting time to wait before printing out the thread stacks to logs
     * @return bi function
     */
    private static BiFunction<? super Object, Throwable, ? extends Void> getShutDownHandler(final BuildMode buildMode,
                                                                                            final Set<Integer> strategyIds,
                                                                                            final ExecutorService zabbixExecutorService,
                                                                                            final ExecutorService exposureCollectorsExecutorService,
                                                                                            final ExecutorService exposureSenderExecutorService,
                                                                                            final ExecutorService tradeExecutorExecutorService,
                                                                                            final ExecutorService messageProcessorExecutorService,
                                                                                            final ExecutorService conversionPumpExecutorService,
                                                                                            final List<IMessageProcessor> processors,
                                                                                            final List<ExposureSender> exposureSenders,
                                                                                            final ExposuresCollector expoCollector,
                                                                                            final TradeExecutor tradeExecutor,
                                                                                            final CurrencyConversionPump conversionPump,
                                                                                            final ZabbixHandler zabbixHandler,
                                                                                            final long timeToWaitInMillisForThreadPrinting) {

        return (result, throwable) -> {
            final Throwable finalThrowable;
            if (throwable != null) {
                finalThrowable = throwable;
            }
            else {
                //This means some task ended without an exception, this should never happen.
                finalThrowable = new RuntimeException("A task completed without an exception. We should not get here. No task should complete. Check logs for which task printed out that it ended.");
            }

            try {
                handleJFundExecutionException(buildMode, finalThrowable, strategyIds);
            }
            finally {

                cleanUp(
                    zabbixExecutorService,
                    zabbixHandler,
                    exposureCollectorsExecutorService,
                    expoCollector,
                    exposureSenderExecutorService,
                    exposureSenders,
                    tradeExecutorExecutorService,
                    tradeExecutor,
                    messageProcessorExecutorService,
                    processors,
                    conversionPumpExecutorService,
                    conversionPump,
                    timeToWaitInMillisForThreadPrinting);
            }
            return null;
        };
    }

    /**
     * Process the exception that brought down jfund
     *
     * @param buildMode  the buildmode we are running in
     * @param ex         exception that brought down jfund
     * @param strategies the strategies we are currently running
     */
    private static void handleJFundExecutionException(final BuildMode buildMode, final Throwable ex, final Set<Integer> strategies) {

        if (buildMode.isRelease()) {
            //For this type of exception we just restart
            if (!isMessageProcessorDisconnect(ex)) {

                NotificationUtils.notifyAdmins(NotificationLevel.FULL, "jFund has abruptly stopped please investigate. It will be restarted now.", ex);
            }
            else {
                NotificationUtils.notifyAdmins(NotificationLevel.EMAIL_ONLY, String.format("jFund running strategies %s has initiated a restart due to losing a connection from mt4.", strategies.toString()), ex);
            }
        }
        logger.error("JFund was stopped.", ex);
    }

    /**
     * Is the cause of this exception due to message processor disconnecting?
     *
     * @param ex wrapper exception
     * @return true if this exception was caused by message processor disconnect
     */
    private static boolean isMessageProcessorDisconnect(final Throwable ex) {
        final Throwable cause = ex.getCause();
        return cause != null && cause.getClass() == MessageProcessorConnectionLostException.class;
    }

    private static List<ExposureSender> createExposureSenders(final List<Integer> strategies, final Map<Integer, BlockingQueue<byte[]>> strategyToExposureSenderQueueMap) {

        final List<ExposureSender> senders = new ArrayList<>();

        for (final Integer strategyId : strategies) {
            senders.add(ExposureSender.create(strategyId, strategyToExposureSenderQueueMap.get(strategyId)));
        }

        return senders;
    }

    private static Map<String, TickInfoInternal> subscribeToSymbolsAndRetrieveTicks(final JManagerServerApiModule jManagerServerApiModule, final MessageProcessor processor, final List<String> symbolsToRegister) {

        final PumpingManager jFundPumpingManager = processor.getPumpingManager();

        symbolsToRegister.forEach(jFundPumpingManager::addSymbol);

        final ServerDetails serverDetails = processor.getServerDetails();

        final Either<WebServiceError, FetchRatesResponse> lastTicksEither = jManagerServerApiModule.fetchRates(serverDetails.getServerId(), symbolsToRegister, sFundXmUserId, SystemProperties.getMachineIP(), CommandVersion.V1,
            WebServiceCallConfig.sDefaultWebServiceCallConfig);
        final List<SymbolRateWithDigits> symbolRateWithDigitsList = lastTicksEither.applyOrElseThrow(error -> new RuntimeException(lastTicksEither.getLeft().getErrorAsThrowable()), FetchRatesResponse::getSymbols);
        final Map<String, SymbolRate> lastTicks = symbolRateWithDigitsList.stream().collect(Functionals.toHashMap(s -> s.mSymbol, s -> SymbolRate.create(s.mDirection, s.mSymbol, s.mBid, s.mAsk, s.mDate)));

        return lastTicks.entrySet()
            .stream()
            .map(entry -> {
                final String symbolName = entry.getKey();
                final SymbolRate symbolRate = entry.getValue();
                return Pair.create(symbolName, TickInfoInternal.create(symbolRate.getBid(), symbolRate.getAsk()));
            })
            .collect(Functionals.toConcurrentHashMap(Pair::getFirst, Pair::getSecond));
    }

    private static final int sWaitTimeToRestartInMilliseconds = 5000;

    private static void handleExecutorShutDown(final ExecutorService executorService, final long timeToWaitBeforePrintingThreadsInMillis) {
        boolean hasShutDown = false;
        do {
            executorService.shutdown();
            try {
                hasShutDown = executorService.awaitTermination(timeToWaitBeforePrintingThreadsInMillis, TimeUnit.MILLISECONDS);
                if (!hasShutDown) {
                    ThreadUtils.logThreadStacks();
                }
            }
            catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!hasShutDown);
    }

    private static void cleanUp(
        final ExecutorService zabbixExecutorService,
        final ZabbixHandler zabbixHandler,
        final ExecutorService exposureCollectorsExecutorService,
        final ExposuresCollector exposureCollectorThread,
        final ExecutorService exposureSenderExecutorService,
        final List<ExposureSender> exposureSenders,
        final ExecutorService tradeExecutorExecutorService,
        final TradeExecutor tradeExecutor,
        final ExecutorService messageProcessorExecutorService,
        final List<IMessageProcessor> processors,
        final ExecutorService conversionPumpExecutorService,
        final CurrencyConversionPump currencyConversionPump,
        final long timeToWaitBeforePrintingThreadsInMillis) {

        logger.info("Starting to stop and cleanup all resources...");

        logger.info("Signalling tasks to die...");
        processors.forEach(IMessageProcessor::timeToDie);
        exposureSenders.forEach(ExposureSender::timeToDie);
        exposureCollectorThread.timeToDie();
        tradeExecutor.timeToDie();
        currencyConversionPump.timeToDie();
        zabbixHandler.timeToDie();
        logger.info("Finished signalling tasks to die.");

        logger.info("Shutting down Zabbix handler...");
        handleExecutorShutDown(zabbixExecutorService, timeToWaitBeforePrintingThreadsInMillis);
        logger.info("Finished shutting down Zabbix handler.");
        logger.info("Shutting down Exposures Collector...");
        handleExecutorShutDown(exposureCollectorsExecutorService, timeToWaitBeforePrintingThreadsInMillis);
        logger.info("Finished shutting down Exposures Collector.");
        logger.info("Shutting down Exposures Sender executor...");
        handleExecutorShutDown(exposureSenderExecutorService, timeToWaitBeforePrintingThreadsInMillis);
        logger.info("Finished shutting down Exposures Sender executor.");
        logger.info("Shutting down Trade Executor executor...");
        handleExecutorShutDown(tradeExecutorExecutorService, timeToWaitBeforePrintingThreadsInMillis);
        logger.info("Finished Shutting down Trade Executor executor.");
        logger.info("Shutting down Message Processors executor...");
        handleExecutorShutDown(messageProcessorExecutorService, timeToWaitBeforePrintingThreadsInMillis);
        logger.info("Finished shutting down Message Processors executor...");
        logger.info("Shutting down Conversion Pump executor...");
        handleExecutorShutDown(conversionPumpExecutorService, timeToWaitBeforePrintingThreadsInMillis);
        logger.info("Finished shutting down Conversion Pump executor.");

        logger.info("Cleaning up message processor reasources...");
        processors.forEach(IMessageProcessor::cleanup);
        logger.info("Finished cleaning up message processor resources.");

        logger.info("Finished stopping and cleaning up all resources.");
    }

    private static int getMessageRequest() {
        return PumpingManager.sRequestConnectionEstablished
            | PumpingManager.sRequestConnectionLost
            | PumpingManager.sRequestTradeOpened
            | PumpingManager.sRequestTradeClosed
            | PumpingManager.sRequestTradeDeletedByAdmin
            | PumpingManager.sRequestTradeUpdated
            | PumpingManager.sRequestSymbolUpdated;
    }

    private static Map<String, StrategySymbolLimits> initiateCurrentActiveStrategySymbolLimitsPerStrategyAndSymbol(
        final Map<Integer, StrategyParameters> strategyParametersMap) {
        final Map<String, StrategySymbolLimits> activeStrategySymbolLimitsPerStrategyAndSymbol = new HashMap<>();
        for (final StrategyParameters sp : strategyParametersMap.values()) {
            for (final Map<StrategySymbolLimits.TYPE, StrategySymbolLimits> st : sp.getSymbolLimits().values()) {
                final String strategyIdAndSymbolKey = KeyGenerator.getStrategyIDAndSymbolKey(st.get(TYPE.STANDARD).getStrategyId(), st.get(TYPE.STANDARD).getSymbolName());

                activeStrategySymbolLimitsPerStrategyAndSymbol.put(strategyIdAndSymbolKey, st.get(TYPE.STANDARD));
            }
        }

        CurrentActiveThresholdTypeAuditHandler.initializeCurrentActiveStrategySymbolLimits(strategyParametersMap);

        return activeStrategySymbolLimitsPerStrategyAndSymbol;
    }
}
