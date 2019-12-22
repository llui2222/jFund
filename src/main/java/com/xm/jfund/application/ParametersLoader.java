package com.xm.jfund.application;

import com.xm.jfund.controllers.DecisionControllerType;
import com.xm.jfund.db.JFundDB;
import com.xm.jfund.utils.LogExposuresMode;
import com.xm.jfund.utils.StrategyExecutionInfo;
import jxmUtils.ApplicationProperties;
import jxmUtils.BuildMode;
import jxmUtils.DBUtils;
import jxmUtils.EmailSender.SendMode;
import jxmUtils.Functionals;
import jxmUtils.StringUtils;
import jxmUtils.TupleModule.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ParametersLoader {

    private static final Logger logger = LoggerFactory.getLogger(ParametersLoader.class);

    public static ParametersLoader create(final String parameterFilePath) {

        final ApplicationProperties sProperties = ApplicationProperties.create(parameterFilePath);

        final Supplier<RuntimeException> missingPropertySupplier = () -> new RuntimeException("Property missing!");

        final SendMode notificationMode = SendMode.getNotificationSendMode(sProperties.getProperty("notificationMode").orElseThrow(missingPropertySupplier));
        final BuildMode buildMode = BuildMode.fromString(sProperties.getProperty("buildMode").orElseThrow(missingPropertySupplier));
        final List<Integer> strategies = sProperties.getIntegerListProperty("strategies", ",");
        final LogExposuresMode logExposuresMode = LogExposuresMode.fromString(sProperties.getProperty("logExposures", "full"));
        final DecisionControllerType decisionControllerType = DecisionControllerType.forType(sProperties.getProperty("decisionController").orElseThrow(missingPropertySupplier));
        final List<Integer> removedServers = sProperties.getIntegerListProperty("removeServers", ",");
        final String zabbixHostName = sProperties.getProperty("zabbixHostName").orElseThrow(missingPropertySupplier);
        final int zabbixPort = sProperties.getIntegerProperty("zabbixPort").orElseThrow(missingPropertySupplier);
        final String zabbixProjectHost = sProperties.getProperty("zabbixProjectHost").orElseThrow(missingPropertySupplier);
        final String zabbixDiscoveryTrapper = sProperties.getProperty("zabbixDiscoveryTrapper").orElseThrow(missingPropertySupplier);
        final String zabbixExposuresTrapperTemplate = sProperties.getProperty("zabbixExposuresTrapperTemplate").orElseThrow(missingPropertySupplier);
        final String zabbixTradesTrapperTemplate = sProperties.getProperty("zabbixTradesTrapperTemplate").orElseThrow(missingPropertySupplier);
        final String zabbixRequestsTrapperTemplate = sProperties.getProperty("zabbixRequestsTrapperTemplate").orElseThrow(missingPropertySupplier);
        final String tradeServiceBaseUrl = sProperties.getProperty("tradeServiceBaseUrl").orElseThrow(missingPropertySupplier);
        final String tradeServiceSocketUrl = sProperties.getProperty("tradeServiceSocketUrl").orElseThrow(missingPropertySupplier);
        final String tradeServiceUsername = sProperties.getProperty("tradeServiceUsername").orElseThrow(missingPropertySupplier);
        final String tradeServicePassword = sProperties.getProperty("tradeServicePassword").orElseThrow(missingPropertySupplier);
        final String dbUrl = sProperties.getProperty("dbUrl").orElseThrow(missingPropertySupplier);
        final String dbUser = sProperties.getProperty("dbUser").orElseThrow(missingPropertySupplier);
        final String dbPassword = sProperties.getProperty("dbPassword").orElseThrow(missingPropertySupplier);

        if (strategies.isEmpty()) {
            throw new RuntimeException("Failed to load strategies from the configuration file.");
        }

        return new ParametersLoader(
            notificationMode,
            buildMode,
            strategies,
            logExposuresMode,
            decisionControllerType,
            removedServers,
            zabbixHostName,
            zabbixPort,
            zabbixProjectHost,
            zabbixDiscoveryTrapper,
            zabbixExposuresTrapperTemplate,
            zabbixTradesTrapperTemplate,
            zabbixRequestsTrapperTemplate,
            tradeServiceBaseUrl,
            tradeServiceSocketUrl,
            tradeServiceUsername,
            tradeServicePassword,
            JFundDB.create(dbUrl, dbUser, dbPassword));
    }

    private static final String sStrategyExecutionTable = "fund_strategy.StrategyExecutionInfo";

    public Map<Integer, StrategyExecutionInfo> getExecutionInfo() throws SQLException {

        final String strategiesAsCsv = StringUtils.listToCsv(getStrategies(), ",", "(", ")");

        final String selectExecutionInfo = String.format("select strategyId, strategyWeight, tradeVolumeThreshold, indicatorThreshold, takerLogin, takerName, reason_codes from %s where strategyId in %s", sStrategyExecutionTable, strategiesAsCsv);

        try (final Connection connectJFund = DriverManager.getConnection(mJFundDB.getUrl(), mJFundDB.getUsername(), mJFundDB.getPassword());
             final PreparedStatement pstmt = connectJFund.prepareStatement(selectExecutionInfo)) {

            return DBUtils.executeSelectQuery(pstmt,
                (ResultSet rs) -> Pair.create(rs.getInt(1), StrategyExecutionInfo.create(rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), rs.getString(5), rs.getString(6), convertReasonCodes(rs.getString(7)))))
                .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));
        }
    }

    static Set<Integer> convertReasonCodes(final String reasonCodes) {
        final Set<Integer> codes;
        if (reasonCodes != null && !reasonCodes.trim().isEmpty()) {
            final String[] split = reasonCodes.trim().split("\\|");
            codes = Arrays.stream(split).
                map(String::trim).
                filter(value -> value.matches("[0-7]")).
                map(Integer::parseInt).
                collect(Collectors.toSet());
        }
        else {
            codes = new HashSet<>();
        }
        logger.info("Reason codes supplied: {}, Reason codes parsed: {}", reasonCodes, codes);
        return codes;
    }

    public SendMode getNotificationMode() {
        return mNotificationMode;
    }

    public BuildMode getBuildMode() {
        return mBuildMode;
    }

    public List<Integer> getStrategies() {
        return mStrategies;
    }

    public LogExposuresMode getLogExposuresMode() {
        return mLogExposuresMode;
    }

    public DecisionControllerType getDecisionControllerType() {
        return mDecisionControllerType;
    }

    public List<Integer> getRemovedServers() {
        return mRemovedServers;
    }

    public String getZabbixHostName() {
        return mZabbixHostName;
    }

    public int getZabbixPort() {
        return mZabbixPort;
    }

    public String getZabbixProjectHost() {
        return mZabbixProjectHost;
    }

    public String getZabbixDiscoveryTrapper() {
        return mZabbixDiscoveryTrapper;
    }

    public String getZabbixExposuresTrapperTemplate() {
        return mZabbixExposuresTrapperTemplate;
    }

    public String getZabbixTradesTrapperTemplate() {
        return mZabbixTradesTrapperTemplate;
    }

    public String getZabbixRequestsTrapperTemplate() {
        return mZabbixRequestsTrapperTemplate;
    }

    public String getTradeServiceBaseUrl() {
        return mTradeServiceBaseUrl;
    }

    public String getTradeServiceSocketUrl() {
        return mTradeServiceSocketUrl;
    }

    public String getTradeServiceUsername() {
        return mTradeServiceUsername;
    }

    public String getTradeServicePassword() {
        return mTradeServicePassword;
    }

    public JFundDB getJFundDB() {
        return mJFundDB;
    }

    @Override
    public String toString() {
        return "ParametersLoader{" +
            "mNotificationMode=" + mNotificationMode +
            ", mBuildMode=" + mBuildMode +
            ", mStrategies=" + mStrategies +
            ", mLogExposuresMode=" + mLogExposuresMode +
            ", mRemovedServers=" + mRemovedServers +
            ", mZabbixHostName='" + mZabbixHostName + '\'' +
            ", mZabbixPort=" + mZabbixPort +
            ", mZabbixProjectHost='" + mZabbixProjectHost + '\'' +
            ", mZabbixDiscoveryTrapper='" + mZabbixDiscoveryTrapper + '\'' +
            ", mZabbixExposuresTrapperTemplate='" + mZabbixExposuresTrapperTemplate + '\'' +
            ", mZabbixTradesTrapperTemplate='" + mZabbixTradesTrapperTemplate + '\'' +
            ", mZabbixRequestsTrapperTemplate='" + mZabbixRequestsTrapperTemplate + '\'' +
            ", mStrategyServiceBaseUrl='" + mTradeServiceBaseUrl + '\'' +
            ", mTradeServiceSocketUrl='" + mTradeServiceSocketUrl + '\'' +
            ", mTradeServiceUsername='" + mTradeServiceUsername + '\'' +
            '}';
    }

    private ParametersLoader(
        final SendMode notificationMode,
        final BuildMode buildMode,
        final List<Integer> strategies,
        final LogExposuresMode logExposuresMode,
        final DecisionControllerType decisionControllerType,
        final List<Integer> removedServers,
        final String zabbixHostName,
        final int zabbixPort,
        final String zabbixProjectHost,
        final String zabbixDiscoveryTrapper,
        final String zabbixExposuresTrapperTemplate,
        final String zabbixTradesTrapperTemplate,
        final String zabbixRequestsTrapperTemplate,
        final String tradeServiceBaseUrl,
        final String tradeServiceSocketUrl,
        final String tradeServiceUsername,
        final String tradeServicePassword,
        final JFundDB jFundDB) {

        mNotificationMode = notificationMode;
        mBuildMode = buildMode;
        mStrategies = strategies;
        mLogExposuresMode = logExposuresMode;
        mDecisionControllerType = decisionControllerType;
        mRemovedServers = removedServers;
        mZabbixHostName = zabbixHostName;
        mZabbixPort = zabbixPort;
        mZabbixProjectHost = zabbixProjectHost;
        mZabbixDiscoveryTrapper = zabbixDiscoveryTrapper;
        mZabbixExposuresTrapperTemplate = zabbixExposuresTrapperTemplate;
        mZabbixTradesTrapperTemplate = zabbixTradesTrapperTemplate;
        mZabbixRequestsTrapperTemplate = zabbixRequestsTrapperTemplate;
        mTradeServiceBaseUrl = tradeServiceBaseUrl;
        mTradeServiceSocketUrl = tradeServiceSocketUrl;
        mTradeServiceUsername = tradeServiceUsername;
        mTradeServicePassword = tradeServicePassword;
        mJFundDB = jFundDB;
    }

    private final SendMode mNotificationMode;
    private final BuildMode mBuildMode;
    private final List<Integer> mStrategies;
    private final LogExposuresMode mLogExposuresMode;
    private final DecisionControllerType mDecisionControllerType;
    private final List<Integer> mRemovedServers;
    private final String mZabbixHostName;
    private final int mZabbixPort;
    private final String mZabbixProjectHost;
    private final String mZabbixDiscoveryTrapper;
    private final String mZabbixExposuresTrapperTemplate;
    private final String mZabbixTradesTrapperTemplate;
    private final String mZabbixRequestsTrapperTemplate;
    private final String mTradeServiceBaseUrl;
    private final String mTradeServiceSocketUrl;
    private final String mTradeServiceUsername;
    private final String mTradeServicePassword;
    private final JFundDB mJFundDB;
}
