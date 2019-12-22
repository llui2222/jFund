package com.xm.jfund.controllers;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class VolumeCalculationValidatorParameterizedTest {

    private final double orderVolume;
    private final double netJfundCoverage;
    private final double dangerExposureLevel;
    private final boolean expected;

    public VolumeCalculationValidatorParameterizedTest(final double orderVolume,
                                                       final double netJfundCoverage,
                                                       final double dangerExposureLevel,
                                                       final boolean expected) {
        this.orderVolume = orderVolume;
        this.netJfundCoverage = netJfundCoverage;
        this.dangerExposureLevel = dangerExposureLevel;
        this.expected = expected;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {-1_000_000.0, -1_000_001, 2_000_000, true},
            {-1000000.0, -1_000_000, 2_000_000, false},
            {1000000.0, -1_000_001, 2_000_000, false},
            {0, -1_000_001, 1_000_000, true},
            {0, 1_000_001, 1_000_000, true},
            {1000000, 0, 0, true},
            {1000000, 1_000_000, 0, true},
            {1000000, 1_000_000, 999_999, true},
            //rounding up, without round, we should have exposure = 1999999.999 + 1000000 = 2999999.999, danger level = 2999999, but with rounding we'll have 3000000 > 2999999.999
            {1999999.999, 1_000_000, 2_999_999.999, false},
        });
    }


    @Test
    public void testDangerExposureLevel() {
        Assert.assertThat(VolumeCalculationValidator.isVolumeAtDangerousExposureLevel(orderVolume, netJfundCoverage, dangerExposureLevel), CoreMatchers.is(expected));
    }
}