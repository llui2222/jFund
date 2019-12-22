package com.xm.jfund.utils;

public class KeyGenerator {
    public static String getStrategyIDAndSymbolKey(final int strategyId, final String symbol) {
        return String.format("%d%s", strategyId, symbol);
    }
}
