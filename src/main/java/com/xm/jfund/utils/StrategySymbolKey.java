package com.xm.jfund.utils;

import java.util.Objects;

/**
 * Key made up of a symbol and a strategy
 */
public final class StrategySymbolKey {
    private final String symbol;
    private final int strategyId;

    private StrategySymbolKey(final int strategyId, final String symbol) {
        this.strategyId = strategyId;
        this.symbol = symbol;
    }

    public static StrategySymbolKey create(final int strategyId, final String symbol) {
        return new StrategySymbolKey(strategyId, symbol);
    }

    @Override
    public String toString() {
        return "StrategySymbolKey{" +
            "symbol='" + symbol + '\'' +
            ", strategyId=" + strategyId +
            '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StrategySymbolKey that = (StrategySymbolKey) o;
        return strategyId == that.strategyId &&
            Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {

        return Objects.hash(symbol, strategyId);
    }
}