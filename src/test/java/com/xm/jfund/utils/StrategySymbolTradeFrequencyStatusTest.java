package com.xm.jfund.utils;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;

public class StrategySymbolTradeFrequencyStatusTest {

    private StrategySymbolTradeFrequencyStatus getStratSymbol(final int numberOfTrades) {
        return StrategySymbolTradeFrequencyStatus.create(1, "name", LocalDateTime.now(), numberOfTrades, null);
    }

    /**
     * Test custom equals
     */
    @Test
    public void testNotEqualsBasedOnNumberOfTrades() {
        final StrategySymbolTradeFrequencyStatus status1 = getStratSymbol(1);
        final StrategySymbolTradeFrequencyStatus status2 = getStratSymbol(5);
        assertFalse(status1.equals(status2));
    }
}
