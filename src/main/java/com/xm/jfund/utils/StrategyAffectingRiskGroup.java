package com.xm.jfund.utils;

import com.xm.jfund.riskgroup.RiskGroupWithExpFactor;

/**
 * Created by msamatas on 06/09/17.
 */
public final class StrategyAffectingRiskGroup {

    public static StrategyAffectingRiskGroup create(final int strategyId, final RiskGroupWithExpFactor riskGroupWithExpFactor) {
        return new StrategyAffectingRiskGroup(strategyId, riskGroupWithExpFactor);
    }

    public int getStrategyId() {
        return mStrategyId;
    }

    public RiskGroupWithExpFactor getRiskGroupWithExpFactor() {
        return mRiskGroupWithExpFactor;
    }

    @Override
    public String toString() {

        return String.format("StrategyAffectingRiskGroup{StrategyId: %d, RiskGroupWithExpFactor: %s", mStrategyId, mRiskGroupWithExpFactor);
    }

    private StrategyAffectingRiskGroup(final int strategyId, final RiskGroupWithExpFactor riskGroupWithExpFactor) {
        mStrategyId = strategyId;
        mRiskGroupWithExpFactor = riskGroupWithExpFactor;
    }

    private final int mStrategyId;
    private final RiskGroupWithExpFactor mRiskGroupWithExpFactor;
}
