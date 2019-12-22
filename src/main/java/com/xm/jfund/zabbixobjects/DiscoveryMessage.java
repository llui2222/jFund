package com.xm.jfund.zabbixobjects;

import com.xm.jfund.utils.StrategySymbolLimits;
import jManagerUtils.ManagerCommandResponseModule;
import jxmUtils.Functionals;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoveryMessage implements ZabbixMessage {

    public static DiscoveryMessage create(final String discoveryTrapper, final List<StrategySymbolLimits> strategySymbolLimits) {

        final List<Map<String, String>> data = strategySymbolLimits.stream()
            .map(s -> {
                final Map<String, String> parameterMap = new HashMap<>();

                final String strategyIdStr = Integer.toString(s.getStrategyId());

                parameterMap.put(sStrategySymbolParameter, String.format("%s_%s", strategyIdStr, s.getSymbolName()));
                parameterMap.put(sStrategyIdParameter, strategyIdStr);
                parameterMap.put(sWarningExposureLevelParameter, sIdentityDecimalFormater.format(s.getWarningExposureLevel()));
                parameterMap.put(sDangerExposureLevelParameter, sIdentityDecimalFormater.format(s.getDangerExposureLevel()));
                parameterMap.put(sWarningTradeSizeLimitParameter, sIdentityDecimalFormater.format(s.getWarningTradeSizeLimit()));
                parameterMap.put(sDangerTradeSizeLimitParameter, sIdentityDecimalFormater.format(s.getDangerTradeSizeLimit()));
                parameterMap.put(sWarningFrequencyLimitParameter, String.valueOf(s.getWarningFrequencyLevel()));
                parameterMap.put(sDangerFrequencyLimitParameter, String.valueOf(s.getDangerFrequencyLevel()));

                return parameterMap;
            })
            .collect(Functionals.toArrayList(strategySymbolLimits.size()));

        data.get(0).put(sTrackNoDataReceivedParameter, "1");

        final String serializedData = "{\"data\":" + ManagerCommandResponseModule.serialize(data).replace("\\", "") + "}";

        return new DiscoveryMessage(discoveryTrapper, serializedData);
    }

    @Override
    public String getKey() {
        return mDiscoveryTrapper;
    }

    @Override
    public String getValue() {
        return mData;
    }

    private static final DecimalFormat sIdentityDecimalFormater = new DecimalFormat("#"); // Zabbix doesn't parse scientific notation properly, so make sure to send the normal representation

    private static final String sStrategySymbolParameter = "{#STRSYMNAME}";
    private static final String sStrategyIdParameter = "{#STRATEGYID}";
    private static final String sTrackNoDataReceivedParameter = "{#TRACKNODATARECEIVED}";
    private static final String sWarningExposureLevelParameter = "{#WARNEXPOSURE}";
    private static final String sDangerExposureLevelParameter = "{#DANGEREXPOSURE}";
    private static final String sWarningTradeSizeLimitParameter = "{#WARNTRADESIZELIMIT}";
    private static final String sDangerTradeSizeLimitParameter = "{#DANGERTRADESIZELIMIT}";
    private static final String sWarningFrequencyLimitParameter = "{#WARNFREQUENCYLIMIT}";
    private static final String sDangerFrequencyLimitParameter = "{#DANGERFREQUENCYLIMIT}";

    private DiscoveryMessage(final String discoveryTrapper, final String data) {
        mDiscoveryTrapper = discoveryTrapper;
        mData = data;
    }

    private final String mDiscoveryTrapper;
    private final String mData;
}
