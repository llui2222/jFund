package com.xm.jfund.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParametersTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testRequireTrueButFalse() {

        thrown.expect(IllegalArgumentException.class);
        Parameters.requireTrue(false);
    }

    @Test
    public void testRequireTrue() {
        Parameters.requireTrue(true);
    }

    @Test
    public void requireNonEmptyEmptyString() {
        thrown.expect(IllegalArgumentException.class);
        Parameters.requireNonEmpty("");

    }

    @Test
    public void requireNonEmptyNullArray() {
        thrown.expect(IllegalArgumentException.class);
        final String nullValue = null;
        Parameters.requireNonEmpty(nullValue);
    }

    @Test
    public void requireNonEmptyNullValue() {
        thrown.expect(IllegalArgumentException.class);
        Parameters.requireNonEmpty("a", null);
    }

    @Test
    public void requireNonEmptyOneEmptyValue() {
        thrown.expect(IllegalArgumentException.class);
        Parameters.requireNonEmpty("a", "", "b");
    }

    @Test
    public void requireNonNull() {
        thrown.expect(IllegalArgumentException.class);
        Parameters.requireNonNull(new Object(), null, new Object());
    }
}