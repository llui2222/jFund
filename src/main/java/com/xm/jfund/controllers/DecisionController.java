package com.xm.jfund.controllers;

import com.xm.jfund.utils.StrategySymbolLimits;

/**
 * This interface should be implemented by classes that are compiled and loaded at runtime from jFund.
 */
public interface DecisionController {

    /**
     * Given information about the current strategy and exposures, returns if an AntiCoverage trade should be ordered, it's volume and direction.
     *
     * @param sumOfExposures sum of xm client exposures and jfund's exposures
     * @param netJfundCoverage    net coverage jfund currently has
     * @param symbolContractSize   The symbol's contract size.
     * @param strategySymbolLimits The strategy symbol constraints
     * @return An {@link OrderVolumeCalculationResult} contains the volume calculation status and the volume to execute if calculation status is ok.
     */
    OrderVolumeCalculationResult calculateOrderVolume(
        final double sumOfExposures,
        final double netJfundCoverage,
        final double symbolContractSize,
        final StrategySymbolLimits strategySymbolLimits);

    boolean hasPendingTrades(final int strategyId, final String symbol);
}
