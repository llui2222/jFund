package com.xm.jfund.utils;

/**
 * Class to verify input parameters more conveniently
 */
public class Parameters {

    /**
     * Require the statement to be true
     *
     * @param statement to test
     * @throws IllegalArgumentException if statement is false
     */
    public static void requireTrue(final boolean statement) {
        if (!statement) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Require values to be not null and non empty
     *
     * @param values input values
     * @throws IllegalArgumentException if any value is null or empty
     */
    public static void requireNonEmpty(final String... values) {

        if (values == null) {
            throw new IllegalArgumentException();
        }
        for (final String current : values) {
            if (current == null || current.isEmpty()) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Require values to be not null
     *
     * @param values input values
     * @throws IllegalArgumentException if any value is null
     */
    public static void requireNonNull(final Object... values) {

        if (values == null) {
            throw new IllegalArgumentException();
        }
        for (final Object current : values) {
            if (current == null) {
                throw new IllegalArgumentException();
            }
        }
    }
}
