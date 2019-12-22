package com.xm.jfund.exposures;

import com.xm.jfund.utils.StrategySymbolLimits;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class ExposuresCollectorParameterizedTest {

    private final double orderVolume;
    private final double netJfundCoverage;
    private final StrategySymbolLimits limits;
    private final boolean expected;

    public ExposuresCollectorParameterizedTest(final double orderVolume,
                                               final StrategySymbolLimits limits,
                                               final double netJfundCoverage,
                                               final boolean expected) {
        this.orderVolume = orderVolume;
        this.netJfundCoverage = netJfundCoverage;
        this.limits = limits;
        this.expected = expected;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            //zero volume
            {0.0, getLimits(), 1_000_000, false},
            //zero volume again
            {0, getLimits(), 1_000_000, false},
            //zero volume again
            {0.000000, getLimits(), 1_000_000, false},
            // volume that doesn't violate any danger thresholds
            {10000, getLimits(), 1_000_000, true},
            //violates single trade
            {510000, getLimits(), 1_000_000, false},
            //violates exposure
            {1000000.1, getLimits(), 1_000_000, false},
        });
    }

    private static StrategySymbolLimits getLimits() {
        return StrategySymbolLimits.create(1,
            0,
            "EURUSD",
            200_000.0,
            1_000_000.0,
            2_000_000.0,
            400_000.0,
            500_000.0,
            100,
            200,
            10_000.0,
            0);
    }

    @Test
    public void testDangerExposureLevel() {
        Assert.assertThat(ExposuresCollector.isVolumeSafeJustBeforeSendingForExecution(orderVolume, limits, netJfundCoverage), CoreMatchers.is(expected));
    }
}

