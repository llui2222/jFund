/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xm.jfund.utils;

import com.xm.jfund.db.JFundDBConnectionProvider;
import com.xm.jfund.utils.NotificationUtils.NotificationLevel;
import jAnalystUtils.SymbolModule;
import jAnalystUtils.SymbolModule.SymbolMetaData;
import jDbConnections.OracleUsers.AnalystApplicationOracle;
import jDbConnections.oracleSchemata.AnalystApplication;
import jxmUtils.BuildMode;
import jxmUtils.Currency;
import jxmUtils.DBUtils;
import jxmUtils.Functionals;
import jxmUtils.StringUtils;
import jxmUtils.TupleModule.Pair;
import jxmUtils.TupleModule.Triplet;
import mt4jUtils.CurrencyLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author nmichael
 */
public final class DataLoader {

    public static Map<Integer, Map<String, SymbolMetaData>> loadSymbolsOfInterest(final Map<Integer, StrategyParameters> strategyParametersMap) {

        return strategyParametersMap.entrySet().stream()
            .map(entry -> {
                final Integer strategyId = entry.getKey();
                final Map<String, SymbolMetaData> symbolMetaDataMap = entry.getValue().getSymbolLimits().keySet().stream()
                    .map(DataLoader::getSymbolMetaData)
                    .collect(Functionals.toHashMap(SymbolMetaData::getSymbolName, Function.identity()));

                return Pair.create(strategyId, symbolMetaDataMap);
            })
            .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));
    }

    private static SymbolMetaData getSymbolMetaData(final String symbolName) {

        return SymbolModule.getSymbolMetaDataByName(symbolName).orElseThrow(() -> new RuntimeException(String.format("Invalid symbol '%s' found in 'jFundSymbolsOfInterest' table.  Exiting.", symbolName)));
    }

    public static Map<Integer, Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>>> loadStrategySymbolThresholds(final Set<Integer> strategies) throws SQLException {

        final String strategiesAsCsv = StringUtils.streamToCsv(strategies.stream(), ",");
        final String fetchStrategySymbolThresholdsQuery = String.format(sLoadStrategySymbolThresholdsTemplate, strategiesAsCsv);

        try (final Connection connectJFund = JFundDBConnectionProvider.getInstance().getConnection();
             final PreparedStatement pstmt = connectJFund.prepareStatement(fetchStrategySymbolThresholdsQuery)) {

            final List<StrategySymbolLimits> ssts = DBUtils.executeSelectQuery(pstmt, (ResultSet rs) ->
                    StrategySymbolLimits.create(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getDouble(5),
                        rs.getDouble(6),
                        rs.getDouble(7),
                        rs.getDouble(8),
                        rs.getInt(9),
                        rs.getInt(10),
                        rs.getDouble(11),
                        rs.getLong(12))).
                collect(Collectors.toList());

            final Map<Integer, Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>>> strategySymbolThresholds = new HashMap<>();
            for (final StrategySymbolLimits sst : ssts) {

                final int strategyId = sst.getStrategyId();
                final String symbolName = sst.getSymbolName();

                if (!strategySymbolThresholds.containsKey(strategyId)) {
                    strategySymbolThresholds.put(strategyId, new HashMap<>());
                }

                if (!strategySymbolThresholds.get(strategyId).containsKey(symbolName)) {
                    strategySymbolThresholds.get(strategyId).put(symbolName, new HashMap<>());
                }

                strategySymbolThresholds.get(strategyId).get(symbolName).put(sst.getType(), sst);
            }

            return strategySymbolThresholds;
        }
    }

    public static Set<Integer> loadDisabledStrategies() throws SQLException {

        try (final Connection connectJFund = JFundDBConnectionProvider.getInstance().getConnection();
             final PreparedStatement pstmt = connectJFund.prepareStatement("SELECT strategyId FROM fund_strategy.DisabledStrategies")) {

            return DBUtils.executeSelectQuery(pstmt, (ResultSet rs) -> rs.getInt(1)).collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        }
    }

    public static void insertDisabledStrategy(final int strategyId) {

        try (final Connection connectJFund = JFundDBConnectionProvider.getInstance().getConnection();
             final PreparedStatement pstmt = connectJFund.prepareStatement(sInsertDisabledStrategyTemplate)) {

            pstmt.setInt(1, strategyId);
            pstmt.executeUpdate();
        }
        catch (final Throwable ex) {
            NotificationUtils.notifyAdmins(NotificationLevel.FULL, String.format("Disabled strategy %d was not inserted into the DB! It will be loaded again on service restart.", strategyId));
        }
    }

    /**
     * Fetches the mappings of symbolNames to their "LP" names for the known strategies. We have a set of symbols for each LP to facilitate A-Book execution (e.g EURUSD -> EURUSDx)
     * The mapping is on the strategy Id.
     * There can be strategies that have identity mappings for symbols, this is functionally equivalent to them not having explicit mappings in the rest of the application.
     *
     * @param strategyToExecutionInfoMap A map from strategyId to it's execution parameters.
     * @return A map from strategy id to a Map from original symbol name to the mapped symbol name.
     * @throws SQLException in case of DB error.
     */
    public static Map<Integer, Map<String, String>> getStrategyToLPSymbolMap(final Map<Integer, StrategyExecutionInfo> strategyToExecutionInfoMap) throws SQLException {

        final String strategyAsCsv = StringUtils.streamToCsv(strategyToExecutionInfoMap.keySet().stream(), ",");
        final String selectMappingsQuery = String.format(sSelectSymbolMappingsTemplate, strategyAsCsv);

        try (final Connection connectJFund = JFundDBConnectionProvider.getInstance().getConnection();
             final PreparedStatement pstmtGetSymbolMap = connectJFund.prepareStatement(selectMappingsQuery)) {

            return DBUtils.executeSelectQuery(pstmtGetSymbolMap, (ResultSet rs) -> Triplet.create(rs.getInt(3), rs.getString(1), rs.getString(2)))
                .collect(Functionals.groupingByToHashMap(Triplet::getFirst, Collectors.toMap(Triplet::getSecond, Triplet::getThird)));
        }
    }

    /**
     * Returns a mapping from LP symbol (e.g USDJPYx) to exposure symbol (e.g USDJPY) per strategy.
     * The mapping is the reverse of the one given in the strategy definitions
     */
    public static Map<Integer, Map<String, String>> getReverseLpSymbolMappings(final Map<Integer, StrategyParameters> initialMap) {

        return initialMap.entrySet().stream()
            .map(entry -> Pair.create(entry.getKey(), entry.getValue().getSymbolToLpNameConversionMap().entrySet().stream().collect(Functionals.toHashMap(Entry::getValue, Entry::getKey, entry.getValue().getSymbolToLpNameConversionMap().size()))))
            .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond, initialMap.size()));
    }

    public static void loadSymbolMetaData(final BuildMode buildMode) {
        DBUtils.withJDBCConnection(AnalystApplicationOracle.get(buildMode), conn -> {
            SymbolModule.loadSymbolMetaData(conn, AnalystApplication.getSchema(buildMode));
            final List<Pair<Integer, String>> currencyData = CurrencyLoader.loadCurrencyData(conn, AnalystApplication.getSchema(buildMode));
            Currency.populateCurrencies(currencyData);
        });
    }

    private static final String sSelectSymbolMappingsTemplate = "select sm.originalName, sm.mappedName, stratsym.strategy "
        + "from fund_strategy.SymbolMappings sm join fund_strategy.strategy_symbol_mappings stratsym on sm.definitionId = stratsym.SYMBOL_MAPPING_ID "
        + "where stratsym.strategy in (%s) ";

    private static final String sLoadStrategySymbolThresholdsTemplate = "select strategyId, sstType, SymName, threshold, " +
        "warningExposureLevel, dangerExposureLevel, singleTradeWarningLimit, singleTradeDangerLevel, frequencyWarningLevel, frequencyDangerLevel, maxInstrumentsPerSingleTrade, tradeDelayInMillis "
        + "from fund_strategy.StrategySymbolThresholds sst join analyst_application.SymbolDetails sd on sst.symbolId = sd.SymId "
        + "where strategyId in (%s) ";

    private static final String sInsertDisabledStrategyTemplate = "INSERT INTO fund_strategy.DisabledStrategies (strategyId) VALUES (?)";
}
