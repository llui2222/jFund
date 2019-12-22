package com.xm.jfund.application;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParametersLoaderTest {

    @Test
    public void testConvertReasonCodes_edgeCases() {
        assertThat(ParametersLoader.convertReasonCodes("")).isEmpty();
        assertThat(ParametersLoader.convertReasonCodes(null)).isEmpty();
        assertThat(ParametersLoader.convertReasonCodes("    ")).isEmpty();
        assertThat(ParametersLoader.convertReasonCodes("\t\n")).isEmpty();
    }

    @Test
    public void testConvertReasonCodes_singleNumber() {
        assertThat(ParametersLoader.convertReasonCodes("5")).contains(5);
        assertThat(ParametersLoader.convertReasonCodes("|5")).contains(5);
        assertThat(ParametersLoader.convertReasonCodes("|5|")).contains(5);
        assertThat(ParametersLoader.convertReasonCodes("5|")).contains(5);
        assertThat(ParametersLoader.convertReasonCodes("     5")).contains(5);
    }

    @Test
    public void testConvertReasonCodes_manyNumbers() {
        assertThat(ParametersLoader.convertReasonCodes("5   |4|2|3    ||0")).contains(5, 4, 2, 3, 0);
        assertThat(ParametersLoader.convertReasonCodes("|5||||6")).contains(5, 6);
        assertThat(ParametersLoader.convertReasonCodes("5|4|3")).contains(5, 4, 3);
    }

    @Test
    public void testConvertReasonCodes_outOfRange() {
        assertThat(ParametersLoader.convertReasonCodes("-1|100")).isEmpty();
    }
}