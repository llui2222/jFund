package com.xm.jfund.utils;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class StrategyExecutionInfo {

    public static StrategyExecutionInfo create(
        final double strategyWeight,
        final double tradeVolumeThreshold,
        final double indicatorThreshold,
        final String takerLogin,
        final String takerName,
        final Set<Integer> reasonCodes) {

        return new StrategyExecutionInfo(strategyWeight, tradeVolumeThreshold, indicatorThreshold, takerLogin, takerName,new HashSet<>(reasonCodes));
    }

    public double getStrategyWeight() {
        return mStrategyWeight;
    }

    public double getTradeVolumeThreshold() {
        return mTradeVolumeThreshold;
    }

    public double getIndicatorThreshold() {
        return mIndicatorThreshold;
    }

    public String getTakerLogin() {
        return takerLogin;
    }

    public String getTakerName() {
        return takerName;
    }

    public Set<Integer> getReasonCodes() {
        return reasonCodes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StrategyExecutionInfo that = (StrategyExecutionInfo) o;

        return new EqualsBuilder()
            .append(mStrategyWeight, that.mStrategyWeight)
            .append(mTradeVolumeThreshold, that.mTradeVolumeThreshold)
            .append(mIndicatorThreshold, that.mIndicatorThreshold)
            .append(takerLogin, that.takerLogin)
            .append(takerName, that.takerName)
            .append(reasonCodes, that.reasonCodes)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(mStrategyWeight)
            .append(mTradeVolumeThreshold)
            .append(mIndicatorThreshold)
            .append(takerLogin)
            .append(takerName)
            .append(reasonCodes)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("mStrategyWeight", mStrategyWeight)
            .append("mTradeVolumeThreshold", mTradeVolumeThreshold)
            .append("mIndicatorThreshold", mIndicatorThreshold)
            .append("takerLogin", takerLogin)
            .append("takerName", takerName)
            .append("reasonCodes", reasonCodes)
            .toString();
    }

    private StrategyExecutionInfo(
        final double strategyWeight,
        final double tradeVolumeThreshold,
        final double indicatorThreshold,
        final String takerLogin,
        final String takerName,
        final Set<Integer> reasonCodes) {

        mStrategyWeight = strategyWeight;
        mTradeVolumeThreshold = tradeVolumeThreshold;
        mIndicatorThreshold = indicatorThreshold;
        this.takerLogin = takerLogin;
        this.takerName = takerName;
        this.reasonCodes = Collections.unmodifiableSet(reasonCodes);

    }

    private final double mStrategyWeight;
    private final double mTradeVolumeThreshold;
    private final double mIndicatorThreshold;
    private final String takerLogin;
    private final String takerName;
    private final Set<Integer> reasonCodes;
}
