package com.xm.jfund.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class StrategySymbolLimits {

    public static StrategySymbolLimits create(
        final int strategyId,
        final int type,
        final String symbolName,
        final double tradingThreshold,
        final double warningExposureLevel,
        final double dangerExposureLevel,
        final double warningTradeSizeLimit,
        final double dangerTradeSizeLimit,
        final int warningFrequencyLevel,
        final int dangerFrequencyLevel,
        final double maxInstrumentsPerSingleTrade,
        final long tradeDelayInMillis) {
        return new StrategySymbolLimits(strategyId,
            type,
            symbolName,
            tradingThreshold,
            warningExposureLevel,
            dangerExposureLevel,
            warningTradeSizeLimit,
            dangerTradeSizeLimit,
            warningFrequencyLevel,
            dangerFrequencyLevel,
            maxInstrumentsPerSingleTrade,
            tradeDelayInMillis);
    }

    public TYPE getType() {
        return mType;
    }

    public int getStrategyId() {
        return mStrategyId;
    }

    public String getSymbolName() {
        return mSymbolName;
    }

    public double getTradingThreshold() {
        return mTradingThreshold;
    }

    public double getWarningExposureLevel() {
        return mWarningExposureLevel;
    }

    /**
     * Get the level we consider dangerous.
     * We don't want the net trades jfund has made to go above this level
     *
     * @return an absolute value we consider dangerous to go above
     */
    public double getDangerExposureLevel() {
        return mDangerExposureLevel;
    }

    public double getWarningTradeSizeLimit() {
        return mWarningTradeSizeLimit;
    }

    public double getDangerTradeSizeLimit() {
        return mDangerTradeSizeLimit;
    }

    public int getWarningFrequencyLevel() {
        return mWarningFrequencyLevel;
    }

    /**
     * We do not want jfund to trade at a frequency above this level.
     * E.g. we dont' want to make 1000 trades in a minute for example.
     * @return the value we don't want to go above in terms of frequency
     */
    public int getDangerFrequencyLevel() {
        return mDangerFrequencyLevel;
    }

    /**
     *
     * @return the maximum instruments size per trade
     */
    public double getMaxInstrumentsPerSingleTrade() {
        return mMaxInstrumentsPerSingleTrade;
    }

    /**
     *
     * @return time in millis that represents how long to delay a trade for
     */
    public long getTradeDelayInMillis() {
        return mTradeDelayInMillis;
    }

    @Override
    public String toString() {
        return "StrategySymbolLimits{" +
            "mStrategyId=" + mStrategyId +
            ", mType=" + mType +
            ", mSymbolName='" + mSymbolName + '\'' +
            ", mTradingThreshold=" + mTradingThreshold +
            ", mWarningExposureLevel=" + mWarningExposureLevel +
            ", mDangerExposureLevel=" + mDangerExposureLevel +
            ", mWarningTradeSizeLimit=" + mWarningTradeSizeLimit +
            ", mDangerTradeSizeLimit=" + mDangerTradeSizeLimit +
            ", mWarningFrequencyLevel=" + mWarningFrequencyLevel +
            ", mDangerFrequencyLevel=" + mDangerFrequencyLevel +
            ", mMaxInstrumentsPerSingleTrade=" + mMaxInstrumentsPerSingleTrade +
            ", mTradeDelayInMillis=" + mTradeDelayInMillis +
            '}';
    }

    private StrategySymbolLimits(
        final int strategyId,
        final int type,
        final String symbolName,
        final double tradingThreshold,
        final double warningExposureLevel,
        final double dangerExposureLevel,
        final double warningTradeSizeLimit,
        final double dangerTradeSizeLimit,
        final int warningFrequencyLevel,
        final int dangerFrequencyLevel,
        final double maximumVolumePerSingleTrade,
        final long tradeDelayInMillis) {

        mStrategyId = strategyId;
        mType = TYPE.valueOf(type);
        mSymbolName = symbolName;
        mTradingThreshold = tradingThreshold;
        mWarningExposureLevel = warningExposureLevel;
        mDangerExposureLevel = dangerExposureLevel;
        mWarningTradeSizeLimit = warningTradeSizeLimit;
        mDangerTradeSizeLimit = dangerTradeSizeLimit;
        mWarningFrequencyLevel = warningFrequencyLevel;
        mDangerFrequencyLevel = dangerFrequencyLevel;
        mMaxInstrumentsPerSingleTrade = maximumVolumePerSingleTrade;
        mTradeDelayInMillis = tradeDelayInMillis;
    }

    private final int mStrategyId;
    private final TYPE mType;
    private final String mSymbolName;
    private final double mTradingThreshold;
    private final double mWarningExposureLevel;
    private final double mDangerExposureLevel;
    private final double mWarningTradeSizeLimit;
    private final double mDangerTradeSizeLimit;
    private final int mWarningFrequencyLevel;
    private final int mDangerFrequencyLevel;
    private final double mMaxInstrumentsPerSingleTrade;
    private final long mTradeDelayInMillis;

    public enum TYPE {
        STANDARD(0),
        CATCH_UP(1);

        private final int typeAsNum;
        private static final Map<Integer, TYPE> types = new HashMap<>();

        TYPE(final int typeAsNum) {
            this.typeAsNum = typeAsNum;
        }

        int getTypeAsNum() {return typeAsNum;}

        static {
            Arrays.stream(TYPE.values()).forEach(type -> types.put(type.typeAsNum, type));
        }

        public static TYPE valueOf(final int typeAsNum) {
            if (types.containsKey(typeAsNum)) return types.get(typeAsNum);

            throw new RuntimeException(
                "Unknown Strategy Symbol Limit Type requested, numeric value requested: " + typeAsNum + ".");
        }
    }
}
