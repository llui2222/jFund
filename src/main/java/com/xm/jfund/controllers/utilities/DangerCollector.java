package com.xm.jfund.controllers.utilities;

import com.xm.jfund.controllers.VolumeCalculationValidator;
import com.xm.jfund.utils.StrategySymbolLimits;

import java.util.ArrayList;
import java.util.List;

/**
 * class that examines dangers
 */
public final class DangerCollector {

    public static DangerCollector create() {
        return new DangerCollector();
    }

    private DangerCollector() {

    }

    /**
     * Get dangers with current potential volume and current jfun exposure
     *
     * @param limits             symbol limits
     * @param potentialVolumeWithContractSize    potential volume to trade with contract size applied
     * @param netJfundExposure    current jfund exposure
     * @return list of dangers if they exist
     */
    public List<Danger> getDangers(final StrategySymbolLimits limits, final double potentialVolumeWithContractSize, final double netJfundExposure) {

        final List<Danger> dangers = new ArrayList<>();
        if (VolumeCalculationValidator.isVolumeAtDangerousExposureLevel(potentialVolumeWithContractSize, netJfundExposure, limits.getDangerExposureLevel())) {
            dangers.add(Danger.EXPOSURE_EXCEEDS_DANGER_LEVEL);
        }

        if (VolumeCalculationValidator.isVolumeAtDangerousSingleTradeLevel(potentialVolumeWithContractSize, limits.getDangerTradeSizeLimit())) {
            dangers.add(Danger.EXPOSURE_EXCEEDS_SINGLE_TRADE_DANGER);
        }

        return dangers;
    }
}
