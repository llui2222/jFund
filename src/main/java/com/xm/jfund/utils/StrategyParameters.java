package com.xm.jfund.utils;

import com.xm.jfund.riskgroup.RiskGroupWithExpFactor;
import com.xm.jfund.riskgroup.TradingAccountRiskGroup;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StrategyParameters {

    public static StrategyParameters create(
        final StrategyExecutionInfo executionInfo,
        final List<RiskGroupWithExpFactor> riskGroupsWithExposureFactors,
        final double commonClientGroupWeight,
        final Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>> symbolLimits,
        final Map<String, String> symbolToLpNameConversionMap,
        final TradingAccountRiskGroup tradingAccountRiskGroup) {
        return new StrategyParameters(executionInfo, riskGroupsWithExposureFactors, commonClientGroupWeight, symbolLimits, symbolToLpNameConversionMap, tradingAccountRiskGroup);
    }

    public StrategyExecutionInfo getExecutionInfo() {
        return mExecutionInfo;
    }

    public List<RiskGroupWithExpFactor> getRiskGroupsWithExposureFactors() {
        return mRiskGroupsWithExposureFactors;
    }

    public double getCommonClientGroupWeight() {
        return mCommonClientGroupWeight;
    }

    public Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>> getSymbolLimits() {
        return mSymbolLimits;
    }

    public Map<String, String> getSymbolToLpNameConversionMap() {
        return mSymbolToLpNameConversionMap;
    }

    public TradingAccountRiskGroup getTradingAccountRiskGroup() {
        return tradingAccountRiskGroup;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StrategyParameters that = (StrategyParameters) o;
        return Double.compare(that.mCommonClientGroupWeight, mCommonClientGroupWeight) == 0 &&
            Objects.equals(mExecutionInfo, that.mExecutionInfo) &&
            Objects.equals(mRiskGroupsWithExposureFactors, that.mRiskGroupsWithExposureFactors) &&
            Objects.equals(mSymbolLimits, that.mSymbolLimits) &&
            Objects.equals(mSymbolToLpNameConversionMap, that.mSymbolToLpNameConversionMap) &&
            Objects.equals(tradingAccountRiskGroup, that.tradingAccountRiskGroup);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mExecutionInfo, mRiskGroupsWithExposureFactors, mCommonClientGroupWeight, mSymbolLimits, mSymbolToLpNameConversionMap, tradingAccountRiskGroup);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("mExecutionInfo", mExecutionInfo)
            .append("mRiskGroupsWithExposureFactors", mRiskGroupsWithExposureFactors)
            .append("mCommonClientGroupWeight", mCommonClientGroupWeight)
            .append("mSymbolLimits", mSymbolLimits)
            .append("mSymbolToLpNameConversionMap", mSymbolToLpNameConversionMap)
            .append("tradingAccountRiskGroup", tradingAccountRiskGroup)
            .toString();
    }

    private StrategyParameters(
        final StrategyExecutionInfo executionInfo,
        final List<RiskGroupWithExpFactor> riskGroupsWithExposureFactors,
        final double commonClientGroupWeight,
        final Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>> symbolLimits,
        final Map<String, String> symbolToLpNameConversionMap,
        final TradingAccountRiskGroup tradingAccountRiskGroup) {

        mExecutionInfo = executionInfo;
        mRiskGroupsWithExposureFactors = riskGroupsWithExposureFactors;
        mCommonClientGroupWeight = commonClientGroupWeight;
        mSymbolLimits = symbolLimits;
        mSymbolToLpNameConversionMap = symbolToLpNameConversionMap;
        this.tradingAccountRiskGroup = tradingAccountRiskGroup;
    }

    private final StrategyExecutionInfo mExecutionInfo;
    private final List<RiskGroupWithExpFactor> mRiskGroupsWithExposureFactors;
    private final double mCommonClientGroupWeight;
    private final Map<String, Map<StrategySymbolLimits.TYPE, StrategySymbolLimits>> mSymbolLimits;
    private final Map<String, String> mSymbolToLpNameConversionMap;
    private final TradingAccountRiskGroup tradingAccountRiskGroup;
}
