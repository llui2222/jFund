package com.xm.jfund.zabbixobjects;

import java.text.DecimalFormat;

public final class ExposureMessage implements ZabbixMessage {

    public static ExposureMessage create(final String exposuresTrapper, final int strategyId, final String symbolName, final double exposure) {
        return new ExposureMessage(exposuresTrapper, strategyId, symbolName, exposure);
    }

    @Override
    public String getKey() {
        return String.format("%s[%d_%s]", mExposuresTrapper, mStrategyId, mSymbolName);
    }

    @Override
    public String getValue() {
        return sIdentityDecimalFormater.format(mExposure);
    }

    private static final DecimalFormat sIdentityDecimalFormater = new DecimalFormat("#"); // Zabbix doesn't parse scientific notation properly, so make sure to send the normal representation

    private ExposureMessage(final String exposuresTrapper, final int strategyId, final String symbolName, final double exposure) {
        mExposuresTrapper = exposuresTrapper;
        mStrategyId = strategyId;
        mSymbolName = symbolName;
        mExposure = exposure;
    }

    private final String mExposuresTrapper;
    private final int mStrategyId;
    private final String mSymbolName;
    private final double mExposure;
}
