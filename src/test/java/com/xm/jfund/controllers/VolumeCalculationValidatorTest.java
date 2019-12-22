package com.xm.jfund.controllers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class VolumeCalculationValidatorTest {

    @Test
    public void testRoundedExposure() {
        final double volume = 1.235;
        final double contractSize = 1;
        assertTrue(Double.compare(VolumeCalculationValidator.getRoundedExposure(volume, contractSize), 1.24) == 0);
    }

    @Test
    public void testRoundedExposure2() {
        final double volume = 1.2345;
        final double contractSize = 1;
        assertTrue(Double.compare(VolumeCalculationValidator.getRoundedExposure(volume, contractSize), 1.23) == 0);
    }

    @Test
    public void testSingleTradeDangerLimit() {
        final double volume = -1001;
        assertTrue(VolumeCalculationValidator.isVolumeAtDangerousSingleTradeLevel(volume, 1000));
    }
}