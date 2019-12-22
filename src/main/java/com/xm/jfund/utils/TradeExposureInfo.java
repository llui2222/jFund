package com.xm.jfund.utils;

import java.util.List;

/**
 * Created by msamatas on 16/06/16.
 */
public final class TradeExposureInfo {

    public static TradeExposureInfo create(final int cmd, final List<TradeExposureOnStrategy> tradeExposureOnStrategies) {

        return new TradeExposureInfo(cmd, tradeExposureOnStrategies);
    }

    public int getCmd() {
        return mCmd;
    }

    public List<TradeExposureOnStrategy> getTradeExposureOnStrategies() {
        return mTradeExposureOnStrategies;
    }

    private TradeExposureInfo(final int cmd, final List<TradeExposureOnStrategy> tradeExposureOnStrategies) {
        mCmd = cmd;
        mTradeExposureOnStrategies = tradeExposureOnStrategies;
    }

    private final int mCmd;
    private final List<TradeExposureOnStrategy> mTradeExposureOnStrategies;
}
