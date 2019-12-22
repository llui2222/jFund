package com.xm.jfund.controllers.utilities;

import com.xm.jfund.math.MathUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for the default controller
 */
public final class DefaultControllerUtilities {

    private DefaultControllerUtilities() {
    }


    /**
     * Get volume of trade, round down
     *
     * @param sumOfExposures     sum of exposures we are paying attention to
     * @param symbolContractSize contract size for symbols (lot size)
     * @return anti volume of current exposures relative to lot size
     */
    public static double getAntiVolumeToTrade(final double sumOfExposures, final double symbolContractSize) {
        return BigDecimal.valueOf(-sumOfExposures / symbolContractSize).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
    }

    /**
     * Get list of volumes to trade given a maximum instrument size per single trade and the current total instruments we want to trade
     * The first volume is not delayed for trading
     *
     * @param totalInstrumentsToTrade      total instruments we want to trade, must be > 0
     * @param maxInstrumentsPerSingleTrade max instruments per single trade, must be >= 0
     * @param symbolContractSize           contract size for a symbol, must be > 0
     * @param sign                         sign of volume
     * @return a list of volumes split up using max instruments per trade
     */
    public static List<Double> splitTotalInstrumentsIntoMaxVolumePerTrade(final double totalInstrumentsToTrade, final double maxInstrumentsPerSingleTrade, final double symbolContractSize, final double sign) {

        validate(totalInstrumentsToTrade, maxInstrumentsPerSingleTrade, symbolContractSize, sign);

        final List<Double> listOfVolumes = new LinkedList<>();

        if (maxInstrumentsPerSingleTrade < totalInstrumentsToTrade) {

            final long amount = getHowManyDivide(totalInstrumentsToTrade, maxInstrumentsPerSingleTrade);

            listOfVolumes.addAll(getVolumes(amount, maxInstrumentsPerSingleTrade));

            final double lastVolume = getLastVolume(totalInstrumentsToTrade, maxInstrumentsPerSingleTrade, amount);

            listOfVolumes.add(lastVolume);
        }
        else {

            listOfVolumes.add(totalInstrumentsToTrade);
        }

        return filterAndAdjustVolumes(listOfVolumes, symbolContractSize, sign);
    }

    static List<Double> filterAndAdjustVolumes(final List<Double> volumes, final double symbolContractSize, final double sign) {
        return volumes.stream().
            map(v -> v * sign).
            map(v -> DefaultControllerUtilities.roundDownAndApplyContractSize(v, symbolContractSize)).
            filter(v -> !MathUtils.isZero(v)).
            collect(Collectors.toList());
    }

    static void validate(final double totalInstrumentsToTrade, final double maxInstrumentsPerSingleTrade, final double symbolContractSize, final double sign) {
        if (totalInstrumentsToTrade < 0) {
            throw new RuntimeException(String.format("Total instruments must be >= 0: %.2f", totalInstrumentsToTrade));
        }

        if (maxInstrumentsPerSingleTrade <= 0) {
            throw new RuntimeException(String.format("Max instruments per single trade must be > 0: %.2f", maxInstrumentsPerSingleTrade));
        }

        if (symbolContractSize <= 0) {
            throw new RuntimeException(String.format("Symbol contract size must be > 0: %.2f", symbolContractSize));
        }

        if (Math.abs(sign) != 1.0 && Double.compare(sign, 0) != 0) {
            throw new RuntimeException("Sign must be +1 or -1 or 0");
        }
    }

    static List<Double> getVolumes(final long amount, final double instrumentsPerTrade) {
        final List<Double> list = new ArrayList<>();
        for (long i = 0; i < amount; i++) {
            list.add(instrumentsPerTrade);
        }
        return list;
    }

    static double getLastVolume(final double totalInstrumentsToTrade,
                                final double instrumentsPerTrade,
                                final long amountDivided) {

        return totalInstrumentsToTrade - (instrumentsPerTrade * amountDivided);
    }

    static long getHowManyDivide(final double instrumentsToTrade, final double maxInstrumentsPerTrade) {
        return (long) Math.floor(instrumentsToTrade / maxInstrumentsPerTrade);
    }

    public static double roundDownAndApplyContractSize(final double volume, final double symbolContractSize) {

        return MathUtils.getRoundedDownValue(volume / symbolContractSize);
    }

    /**
     * @param value to get opposite of
     * @return value with opposite sign
     */
    public static double getOpposite(final double value) {
        return -value;
    }
}
