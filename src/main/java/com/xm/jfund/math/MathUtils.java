package com.xm.jfund.math;

import java.math.BigDecimal;

/**
 * Utility class to do math related stuff
 */
public final class MathUtils {

    private MathUtils() {

    }

    /**
     * Is the value zero?
     * Note -0.0 is considered 0
     *
     * @param value to check
     * @return true if zero
     */
    public static boolean isZero(final double value) {
        return Double.compare(Math.abs(value), 0.0) == 0;
    }

    /**
     * See {@link java.math.BigDecimal#ROUND_HALF_UP ROUND_HALF_UP}.
     *
     * @param val    Value to round
     * @param digits Digits to round to
     * @return The rounded value
     */
    public static double roundHalfUp(final double val, final int digits) {
        return BigDecimal.valueOf(val).setScale(digits, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * See {@link java.math.BigDecimal#ROUND_DOWN}.
     *
     * @param val    Value to round
     * @param digits Digits to round to
     * @return The rounded value
     */
    private static double roundDown(final double val, final int digits) {
        return BigDecimal.valueOf(val).setScale(digits, BigDecimal.ROUND_DOWN).doubleValue();
    }

    /**
     * Get rounded value
     *
     * @param value to round
     * @return rounded value
     */
    public static double getRoundedValue(final double value) {

        return getSignMultiplier(value) * roundHalfUp(Math.abs(value), 2);
    }

    public static double getRoundedDownValue(final double value) {
        return getSignMultiplier(value) * roundDown(Math.abs(value), 2);
    }


    /**
     * Get sign to multiply by
     *
     * @param value value that dictates the sign to multiply by
     * @return -1 for negative numbers, 1 for non-negative, 0 for 0
     */
    public static double getSignMultiplier(final double value) {
        final double result;
        final double signum = Math.signum(value);
        //can be -0.0 here
        if (Double.compare(Math.abs(signum), 0) == 0) {
            result = 0;
        }
        else {
            result = signum;
        }
        return result;
    }
}
