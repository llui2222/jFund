package com.xm.jfund.utils;

import jxmUtils.Numeric;
import jxmUtils.exposureInterchange.SymbolExposureComputedValues;
import jxmUtils.exposureInterchange.SymbolExposureEntry;

/**
 * Created by msamatas on 06/03/17.
 */
public final class DecisionUtils {

    public static double getSumOfExposures(final SymbolExposureComputedValues symbolExposureComputedValues) {

        final double clientTotalWeightedTradeVolume = symbolExposureComputedValues.mNetTrades;
        final double totalXMTradeVolume = symbolExposureComputedValues.mNetCoverage;

        return clientTotalWeightedTradeVolume + totalXMTradeVolume;
    }

    public static boolean exposuresExceedIndicatorThreshold(final double indicatorValue, final double indicatorThreshold) {

        return indicatorValue >= indicatorThreshold;
    }

    public static boolean exposuresExceedSymbolThreshold(final double sumOfExposures, final double symbolTradingVolumeThreshold) {

        return Math.abs(sumOfExposures) >= symbolTradingVolumeThreshold;
    }

    public static double calculateIndicatorValue(final SymbolExposureEntry symbolExposureEntry, final double commonGroupWeight, final double riskAppetiteFactor) {

        final double strategyClientLongExposures = symbolExposureEntry.mTradeExp.mLongExp;
        final double strategyClientShortExposures = symbolExposureEntry.mTradeExp.mShortExp;

        final double rawClientLongExposures = getExposure(strategyClientLongExposures, commonGroupWeight, riskAppetiteFactor);
        final double rawClientShortExposures = getExposure(strategyClientShortExposures, commonGroupWeight, riskAppetiteFactor);

        final double conversionFactor = symbolExposureEntry.mConvFactor;

        final double clientLongInMillionEuro = getClientTradesInMillionEuro(rawClientLongExposures, conversionFactor);
        final double clientShortInMillionEuro = getClientTradesInMillionEuro(rawClientShortExposures, conversionFactor);

        return Numeric.toFiniteDouble(
            (clientLongInMillionEuro + clientShortInMillionEuro)
                * (clientLongInMillionEuro + clientShortInMillionEuro)
                * Math.min(clientLongInMillionEuro, Math.abs(clientShortInMillionEuro))
                / (clientLongInMillionEuro - clientShortInMillionEuro));
    }

    public static double getClientTradesInMillionEuro(final double clientExposures, final double conversionFactor) {
        return clientExposures * conversionFactor / 1_000_000;
    }

    public static double getExposure(final double strategyExposure, final double commonGroupWeight, final double riskAppetiteFactor) {
        return strategyExposure / commonGroupWeight / riskAppetiteFactor;
    }

    /**
     * Change current strategy execution type to retrieve the correct symbol limits at the time of
     * trade execution
     *
     * @param tradingThreshold threshold of when we can start trading
     * @param dangerExposureLimit danger exposure limit of strategy and symbol
     * @param currentTradingFlow the current trading flow of clients' open trades
     * @param currentAntiCoverage the current trading flow of our open trades
     * @param tradingFlowSign the sign of the currentTradingFlow
     */
    public static boolean isAPotentialStrategySymbolLimitsSwitchToStandard(final double tradingThreshold, final double dangerExposureLimit, final double currentTradingFlow, final double currentAntiCoverage, final double tradingFlowSign)
    {
        return Math.abs((tradingFlowSign * Math.min(Math.abs(currentTradingFlow), dangerExposureLimit)) + currentAntiCoverage) <= tradingThreshold;
    }
}
