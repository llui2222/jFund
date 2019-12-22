package com.xm.jfund.math;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MathUtilsTest {

    @Test
    public void testIsZero() {
        final double value1 = -0.0;
        final double value2 = +0.0;
        final double value3 = 0.0;
        final double value4 = 0.0000000000000000000000000000000000000000001;
        assertTrue(MathUtils.isZero(value1));
        assertTrue(MathUtils.isZero(value2));
        assertTrue(MathUtils.isZero(value3));
        assertFalse(MathUtils.isZero(value4));
    }

    @Test
    public void testRoundHalfUp() {
        final double value1 = 1.555;
        final double value2 = 1.553;
        final double value3 = -1.555123;

        assertTrue(Double.compare(MathUtils.roundHalfUp(value1, 2), 1.56) == 0);
        assertTrue(Double.compare(MathUtils.roundHalfUp(value2, 2), 1.55) == 0);
        assertTrue(Double.compare(MathUtils.roundHalfUp(value3, 2), -1.56) == 0);
    }

    @Test
    public void testGetRoundedValue() {
        final double value1 = -1.235;
        final double value2 = 1.235;
        final double value3 = 1.234;
        final double value4 = 1.23545454545;

        assertTrue(Double.compare(MathUtils.getRoundedValue(value1), -1.24) == 0);
        assertTrue(Double.compare(MathUtils.getRoundedValue(value2), 1.24) == 0);
        assertTrue(Double.compare(MathUtils.getRoundedValue(value3), 1.23) == 0);
        assertTrue(Double.compare(MathUtils.getRoundedValue(value4), 1.24) == 0);
    }

    @Test
    public void testGetSignMultiplier() {
        final double value1 = -0.0000000001;
        final double value2 = -0.0;
        final double value3 = +0.0;
        final double value4 = +0.01;
        final double value5 = 1;
        final double value6 = 0;
        final double value7 = -0;
        final double value8 = +0;

        assertTrue(Double.compare(MathUtils.getSignMultiplier(value1), -1) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value2), 0) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value3), 0) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value4), 1) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value5), 1) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value6), 0) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value7), 0) == 0);
        assertTrue(Double.compare(MathUtils.getSignMultiplier(value8), 0) == 0);
    }
}