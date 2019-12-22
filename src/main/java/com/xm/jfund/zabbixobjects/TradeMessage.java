package com.xm.jfund.zabbixobjects;

import java.text.DecimalFormat;

public final class TradeMessage implements ZabbixMessage {

    public static TradeMessage create(final String tradesTrapper, final int strategyId, final String symbolName, final double exposure) {
        return new TradeMessage(tradesTrapper, strategyId, symbolName, exposure);
    }

    @Override
    public String getKey() {
        return String.format("%s[%d_%s]", mTradesTrapper, mStrategyId, mSymbolName);
    }

    @Override
    public String getValue() {
        return sIdentityDecimalFormater.format(mExposure);
    }

    private static final DecimalFormat sIdentityDecimalFormater = new DecimalFormat("#"); // Zabbix doesn't parse scientific notation properly, so make sure to send the normal representation

    private TradeMessage(final String tradesTrapper, final int strategyId, final String symbolName, final double exposure) {
        mTradesTrapper = tradesTrapper;
        mStrategyId = strategyId;
        mSymbolName = symbolName;
        mExposure = exposure;
    }

    private final String mTradesTrapper;
    private final int mStrategyId;
    private final String mSymbolName;
    private final double mExposure;
}
