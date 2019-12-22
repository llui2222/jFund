package com.xm.jfund.utils;

import jAnalystUtils.TradeExposure;

/**
 * Created by msamatas on 06/09/17.
 */
public final class TradeExposureOnStrategy {

    public static TradeExposureOnStrategy create(final int strategyId, final TradeExposure tradeExposure, final String riskGroupName, final boolean isClientRiskGroup) {
        return new TradeExposureOnStrategy(strategyId, tradeExposure, riskGroupName, isClientRiskGroup);
    }

    public int getStrategyId() {
        return mStrategyId;
    }

    public TradeExposure getTradeExposure() {
        return mTradeExposure;
    }

    public String getRiskGroupName() {
        return mRiskGroupName;
    }

    public boolean isClientRiskGroup() {
        return mIsClientRiskGroup;
    }

    private TradeExposureOnStrategy(final int strategyId, final TradeExposure tradeExposure, final String riskGroupName, final boolean isClientRiskGroup) {
        mStrategyId = strategyId;
        mTradeExposure = tradeExposure;
        mRiskGroupName = riskGroupName;
        mIsClientRiskGroup = isClientRiskGroup;
    }

    private final int mStrategyId;
    private final TradeExposure mTradeExposure;
    private final String mRiskGroupName;
    private final boolean mIsClientRiskGroup;
}
