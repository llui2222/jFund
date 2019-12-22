package com.xm.jfund.utils;

import com.xm.jfund.utils.StrategySymbolLimits.TYPE;

import java.time.LocalDateTime;

public final class StrategySymbolTradeFrequencyStatus {

    public static StrategySymbolTradeFrequencyStatus create(final int strategyId, final String symbolName, final LocalDateTime timeUpToMinute, final int numberOfTrades, final TYPE type) {
        return new StrategySymbolTradeFrequencyStatus(strategyId, symbolName, timeUpToMinute, numberOfTrades, type);
    }

    public StrategySymbolTradeFrequencyStatus withIncrement(final int increment) {
        
        return create(mStrategyId, mSymbolName, mTimeUpToMinute, mNumberOfTrades + increment, mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting);
    }

    public int getStrategyId() {
        return mStrategyId;
    }

    public String getSymbolName() {
        return mSymbolName;
    }

    public LocalDateTime getTimeUpToMinute() {
        return mTimeUpToMinute;
    }

    public int getNumberOfTrades() {
        return mNumberOfTrades;
    }

    public TYPE getCurrentExecutionStrategySymbolLimitType() {
        return mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StrategySymbolTradeFrequencyStatus that = (StrategySymbolTradeFrequencyStatus) o;

        if (mStrategyId != that.mStrategyId) {
            return false;
        }
        if (mNumberOfTrades != that.mNumberOfTrades) {
            return false;
        }
        if (!mSymbolName.equals(that.mSymbolName)) {
            return false;
        }
        if (!mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting.equals(that.mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting)) {
            return false;
        }
        return mTimeUpToMinute.equals(that.mTimeUpToMinute);
    }

    @Override
    public int hashCode() {
        int result = mStrategyId;
        result = 31 * result + mSymbolName.hashCode();
        result = 31 * result + mTimeUpToMinute.hashCode();
        result = 31 * result + mNumberOfTrades;
        result = 31 * result + mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StrategySymbolTradeFrequencyStatus{" +
            "mStrategyId=" + mStrategyId +
            ", mSymbolName='" + mSymbolName + '\'' +
            ", mTimeUpToMinute=" + mTimeUpToMinute +
            ", mNumberOfTrades=" + mNumberOfTrades +
            ", mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting=" + mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting +
            '}';
    }

    private StrategySymbolTradeFrequencyStatus(final int strategyId, final String symbolName, final LocalDateTime timeUpToMinute, final int numberOfTrades, final TYPE type) {
        mStrategyId = strategyId;
        mSymbolName = symbolName;
        mTimeUpToMinute = timeUpToMinute;
        mNumberOfTrades = numberOfTrades;
        mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting = type;
    }

    private final int mStrategyId;
    private final String mSymbolName;
    private final LocalDateTime mTimeUpToMinute;
    private final int mNumberOfTrades;
    private final TYPE mExecutionStrategySymbolLimitTypeOfCurrentTradeExecuting;
}
