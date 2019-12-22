package com.xm.jfund.controllers;

import com.xm.jfund.math.MathUtils;

public final class VolumeCalculationValidator {

    private VolumeCalculationValidator() {

    }

    /**
     * Is the order volume too dangerous for us?
     *
     * @param orderVolume         proposed volume to trade
     * @param netJfundExposure    net jfund exposure of a particular strategy
     * @param dangerExposureLevel the danger exposure level
     * @return true if this volume is dangerous
     */
    public static boolean isVolumeAtDangerousExposureLevel(final double orderVolume, final double netJfundExposure, final double dangerExposureLevel) {
        return Math.abs(orderVolume + netJfundExposure) > dangerExposureLevel;
    }

    /**
     * Is volume at a dangerous single trade level?
     *
     * @param orderVolume            volume to trade
     * @param singleTradeDangerLevel single trade danger level
     * @return true if volume is dangerous
     */
    public static boolean isVolumeAtDangerousSingleTradeLevel(final double orderVolume, final double singleTradeDangerLevel) {

        return Math.abs(orderVolume) > singleTradeDangerLevel;
    }

    /**
     * Get rounded exposure. We round up.
     *
     * @param orderVolume        volume to trade
     * @param symbolContractSize contract size
     * @return rounded exposure
     */
    static double getRoundedExposure(final double orderVolume, final double symbolContractSize) {
        return MathUtils.roundHalfUp(orderVolume * symbolContractSize, 2);
    }
}
