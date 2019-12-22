package com.xm.jfund.utils;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class DecisionUtilsTest {

    @Test
    public void testExposuresExceedIndicatorThreshold() {
        final double indicatorValue = 10;
        final double indicatorThreshold = 10;

        assertTrue(DecisionUtils.exposuresExceedIndicatorThreshold(indicatorValue, indicatorThreshold));
    }

    @Test
    public void testExposuresExceedSymbolThreshold() {
        final double sumOfExposures = -10;
        final double symbolTradingVolumeThreshold = 10;

        assertTrue(DecisionUtils.exposuresExceedSymbolThreshold(sumOfExposures, symbolTradingVolumeThreshold));
    }

    @Test
    public void testGetClientTradesInMillionEuro() {
        final double exposures = 10;
        final double conversion = 1.0;
        final double oneMillion = 100 * 100 * 100;
        final double expected = exposures * conversion / oneMillion;

        assertEquals(expected, DecisionUtils.getClientTradesInMillionEuro(exposures, conversion));
    }

    @Test
    public void testGetExposure() {
        final double strategyExposure = 1000;
        final double commonGroupWeight = 2;
        final double riskAppetiteFactor = 5;

        final double expected = strategyExposure / commonGroupWeight / riskAppetiteFactor;

        assertEquals(expected, DecisionUtils.getExposure(strategyExposure, commonGroupWeight, riskAppetiteFactor));
    }

    @Test
    public void testShouldStrategySymbolLimitsChangeToStandard() {
        double tradingThreshold = 10;
        double dangerExposureLimit = 100;
        double tradingFlow = 10;
        double antiCoverage = -10;
        double tradingFlowSign = 1;

        assertTrue(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));

        tradingThreshold = 200_000;
        dangerExposureLimit = 9_000_000;
        tradingFlow = 12_000_000;
        antiCoverage = -9_000_000;
        tradingFlowSign = 1;
        assertTrue(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));

        tradingFlowSign = -1;
        assertFalse(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));

        dangerExposureLimit = 9_000_000;
        tradingFlow = -12_000_000;
        antiCoverage = 0;
        tradingFlowSign = -1;
        assertFalse(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));

        tradingFlowSign = 1;
        assertFalse(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));

        dangerExposureLimit = 12_000_000;
        tradingFlow = -9_000_000;
        antiCoverage = 0;
        tradingFlowSign = -1;
        assertFalse(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));

        dangerExposureLimit = 12_000_000;
        tradingFlow = -9_000_000;
        antiCoverage = 9_000_000;
        tradingFlowSign = -1;
        assertTrue(DecisionUtils.isAPotentialStrategySymbolLimitsSwitchToStandard(tradingThreshold, dangerExposureLimit, tradingFlow, antiCoverage, tradingFlowSign));
    }
}
