package com.xm.jfund.utils;

import jAnalystUtils.SymbolModule.SymbolMetaData;
import jxmUtils.ServerFarm.ServerDetails;
import mt4j.RequestingManager;
import mt4j.ServerException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ValidationUtils {

    public static void ensureExecutionArgumentsAreLoaded(final Map<Integer, StrategyExecutionInfo> strategyToExecutionInfoMap, final List<Integer> requestedStrategies) {

        final Set<Integer> loadedStrategies = strategyToExecutionInfoMap.keySet();

        if (loadedStrategies.size() !=  requestedStrategies.size()){
            final String errorMessage = String.format("Execution arguments were not loaded correctly. Known strategy configurations: %s. Requested strategy configurations: %s", loadedStrategies.toString(), requestedStrategies.toString());
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static final int sOpenAndPendingOrdersFlag = 3;

    private static boolean targetHasOpenTrades(final ServerDetails sd, final long tradingAccount, final int login, final String password) {

        try {
            final RequestingManager requestingManager = RequestingManager.create(login, password, sd.getServerId(), sd.getHostName());
            requestingManager.connect();

            final boolean result = requestingManager.hasOpenTrades((int) tradingAccount, sOpenAndPendingOrdersFlag);
            requestingManager.disconnect();

            return result;
        }
        catch (final ServerException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void ensureSymbolThresholdsWereLoaded(final Map<Integer, Map<String, SymbolMetaData>> strategySymbolsInPlay, final Collection<Integer> strategies) {

        if (strategySymbolsInPlay.isEmpty() || strategySymbolsInPlay.size() != strategies.size()) {

            throw new IllegalArgumentException(String.format("Symbol thresholds were not loaded! Strategies in configuration: %s, Strategies loaded: %s", strategies.toString(), strategySymbolsInPlay.keySet().toString()));
        }
    }
}
