package com.xm.jfund.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrategySymbolKeyTest {

    @Test
    public void testEquals() {
        final StrategySymbolKey key = StrategySymbolKey.create(1, "one");
        final StrategySymbolKey key2 = StrategySymbolKey.create(1, "one");
        final StrategySymbolKey key3 = StrategySymbolKey.create(1, "two");
        final StrategySymbolKey key4 = StrategySymbolKey.create(2, "one");

        assertTrue(key.equals(key2));
        assertFalse(key.equals(key3));
        assertFalse(key.equals(key4));
    }

    @Test
    public void testHashCode() {
        final StrategySymbolKey key = StrategySymbolKey.create(1, "one");
        final StrategySymbolKey key2 = StrategySymbolKey.create(1, "one");
        final StrategySymbolKey key3 = StrategySymbolKey.create(1, "two");
        final StrategySymbolKey key4 = StrategySymbolKey.create(2, "one");

        assertTrue(key.hashCode() == key2.hashCode());
        assertFalse(key.hashCode() == key3.hashCode());
        assertFalse(key.hashCode() == key4.hashCode());
    }
}