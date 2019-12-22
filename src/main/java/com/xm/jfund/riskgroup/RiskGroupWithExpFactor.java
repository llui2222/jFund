/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xm.jfund.riskgroup;

import jAnalystUtils.riskGroups.RiskGroup;

/**
 *
 * @author nmichael
 */
public final class RiskGroupWithExpFactor {

    public static RiskGroupWithExpFactor create(final RiskGroup riskGroup, final double exposureFactor) {
        return new RiskGroupWithExpFactor(riskGroup, exposureFactor);
    }
    
    public RiskGroup getRiskGroup() {
        return mRiskGroup;
    }

    public double getExposureFactor() {
        return mExposureFactor;
    }

    @Override
    public String toString() {

        return String.format("RiskGroupWithExpFactor{RiskGroup: %s, ExposureFactor: %f}",
            mRiskGroup.getGroupName(),
            mExposureFactor);
    }

    private RiskGroupWithExpFactor(final RiskGroup riskGroup, final double exposureFactor) {
        mRiskGroup = riskGroup;
        mExposureFactor = exposureFactor;
    }

    private final RiskGroup mRiskGroup;
    private final double mExposureFactor;
}
