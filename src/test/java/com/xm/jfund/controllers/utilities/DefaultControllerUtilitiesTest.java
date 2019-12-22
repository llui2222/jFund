package com.xm.jfund.controllers.utilities;

import com.xm.jfund.math.MathUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class DefaultControllerUtilitiesTest {

    private static final double symbolContractSize = 100_000;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDivided_sameSize() {

        final double instrumentsToTrade = 40000;

        final long amount = DefaultControllerUtilities.getHowManyDivide(instrumentsToTrade, instrumentsToTrade);
        assertTrue(amount == 1);
    }

    @Test
    public void testGetDivided_largerThanTotalInstruments() {

        final double instrumentsToTrade = 40000;
        final double instrumentsPerTrade = 80000;

        final long amount = DefaultControllerUtilities.getHowManyDivide(instrumentsToTrade, instrumentsPerTrade);
        assertTrue(amount == 0);
    }

    @Test
    public void testGetDivided_largerThanTotalInstrumentsSlightly() {

        final double instrumentsToTrade = 40000;
        final double instrumentsPerTrade = 40000 + 1e-100;
        final long amount = DefaultControllerUtilities.getHowManyDivide(instrumentsToTrade, instrumentsPerTrade);
        assertTrue(amount == 1);
    }

    @Test
    public void testGetDivided_lessButEvenDivide() {

        final double instrumentsToTrade = 40000;
        final double instrumentsPerTrade = 20000;
        final long amount = DefaultControllerUtilities.getHowManyDivide(instrumentsToTrade, instrumentsPerTrade);
        assertTrue(amount == 2);
    }

    @Test
    public void testGetDivided_lessButUnEvenDivide() {

        final double instrumentsToTrade = 40000;
        final double instrumentsPerTrade = 20000 + 1e-200;
        final long amount = DefaultControllerUtilities.getHowManyDivide(instrumentsToTrade, instrumentsPerTrade);
        assertTrue(amount == 2);
    }

    @Test
    public void testGetDivided_lessButUnEvenDivideNoticeable() {

        final double instrumentsToTrade = 40000;
        final double instrumentsPerTrade = 20001;
        final long amount = DefaultControllerUtilities.getHowManyDivide(instrumentsToTrade, instrumentsPerTrade);
        assertTrue(amount == 1);
    }

    @Test
    public void testSplitTotalInstrumentsIntoMaxVolumePerTradeNegative() {
        final double totalInstruments = -10000.15;
        final double maxInstrumentsPerTrade = 10000;
        final List<Double> volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(Math.abs(totalInstruments), maxInstrumentsPerTrade, symbolContractSize, MathUtils.getSignMultiplier(totalInstruments));

        assertTrue(volumes.size() == 1);
        assertTrue(Double.compare(volumes.get(0), -0.1) == 0);
    }

    @Test
    public void testSplitTotalInstrumentsIntoMaxVolumePerTradeMaxIsTooBig() {
        final double totalInstruments = -10000.15;
        final double maxInstrumentsPerTrade = 20000;

        final List<Double> volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(Math.abs(totalInstruments), maxInstrumentsPerTrade, symbolContractSize, MathUtils.getSignMultiplier(totalInstruments));

        assertTrue(volumes.size() == 1);
        assertTrue(Double.compare(volumes.get(0), -0.10) == 0);
    }

    @Test
    public void testSplitTotalInstrumentsIntoMaxVolumePerTradeInstrumentsIsZero() {
        final double totalInstruments = 0;
        final double maxInstrumentsPerTrade = 1;
        final List<Double> volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(totalInstruments, maxInstrumentsPerTrade, symbolContractSize, MathUtils.getSignMultiplier(totalInstruments));

        assertTrue(volumes.isEmpty());
    }

    @Test
    public void testSplitTotalInstrumentsIntoMaxVolumePerTradeInstrumentsIsPositiveZero() {
        final double totalInstruments = +0;
        final double maxInstrumentsPerTrade = 1;
        final List<Double> volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(totalInstruments, maxInstrumentsPerTrade, symbolContractSize, MathUtils.getSignMultiplier(totalInstruments));

        assertTrue(volumes.isEmpty());
    }

    @Test
    public void testSplitTotalInstrumentsIntoMaxVolumePerTradeInstrumentsIsNegativeZero() {
        final double totalInstruments = -0;
        final double maxInstrumentsPerTrade = 1;
        final List<Double> volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(totalInstruments, maxInstrumentsPerTrade, symbolContractSize, MathUtils.getSignMultiplier(totalInstruments));

        assertTrue(volumes.isEmpty());
    }

    @Test
    public void testSplitTotalInstrumentsIntoMaxVolumePerTrade() {
        final double totalInstruments = 1000000.2345;
        final double maxInstrumentsPerTrade = 100000;
        final List<Double> volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(totalInstruments, maxInstrumentsPerTrade, symbolContractSize, MathUtils.getSignMultiplier(totalInstruments));

        assertTrue(Double.compare(volumes.size(), 10) == 0);
        assertTrue(Double.compare(volumes.get(0), 1.0) == 0);
        assertTrue(Double.compare(volumes.get(9), 1.0) == 0);

        final double sum = volumes.stream().mapToDouble(Double::doubleValue).sum();

        assertTrue(Double.compare(sum, 10.0) == 0);
    }

    @Test
    public void getAntiVolumeToTrade() {

        final double sumOfExposures = 100;
        final double contractSize = 10;

        final double antiVolumeToTrade = DefaultControllerUtilities.getAntiVolumeToTrade(sumOfExposures, contractSize);

        final double expected = sumOfExposures / contractSize * -1;

        assertTrue(Double.compare(expected, antiVolumeToTrade) == 0);
    }

    @Test
    public void testAdjustVolumeRounded() {
        final double adjusted = DefaultControllerUtilities.roundDownAndApplyContractSize(-111111, 100000);
        assertTrue(Double.compare(adjusted, -1.11) == 0);
    }

    @Test
    public void testValidateNegativeSingleTrade() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Max instruments per single trade must be > 0: -1.00");
        DefaultControllerUtilities.validate(1, -1, 1, 1);
    }

    @Test
    public void testValidateZeroContractSize() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Symbol contract size must be > 0: 0.00");
        DefaultControllerUtilities.validate(1, 1, 0, 1);
    }

    @Test
    public void testValidateNegativeTotalInstruments() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Total instruments must be >= 0: -1.00");
        DefaultControllerUtilities.validate(-1, -0, 0, 1);
    }

    @Test
    public void testValidateNegativeTotalInstruments_shouldntThrowException() {

        DefaultControllerUtilities.validate(1, 1, 1, MathUtils.getSignMultiplier(-0.0));
    }

    @Test
    public void testGetAdjustedVolumes() {
        final List<Double> adjustedVolumes = DefaultControllerUtilities.getVolumes(2, 11111);

        assertTrue(adjustedVolumes.size() == 2);
        assertTrue(Double.compare(adjustedVolumes.get(0), 11111) == 0);
        assertTrue(Double.compare(adjustedVolumes.get(1), 11111) == 0);
    }

    @Test
    public void testGetAdjustedVolumesZeroAmount() {
        final List<Double> adjustedVolumes = DefaultControllerUtilities.getVolumes(0, 11111);

        assertTrue(adjustedVolumes.size() == 0);
    }

    @Test
    public void testGetLastVolumeWithoutSign_noLastVolume() {
        final double lastVolumeWithoutSign = DefaultControllerUtilities.getLastVolume(10_000, 1_000, 1);
        assertTrue(Double.compare(lastVolumeWithoutSign, 9_000) == 0);
    }

    @Test
    public void testFilterAndAdjustVolumes_zeros() {

        assertTrue(DefaultControllerUtilities.filterAndAdjustVolumes(Arrays.asList(0d, 0d), 1_000, -1).isEmpty());

        assertTrue(DefaultControllerUtilities.filterAndAdjustVolumes(Collections.singletonList(0d), 1_000, -1).isEmpty());
    }

    @Test
    public void testFilterAndAdjustVolumes_roundedToZero() {

        assertTrue(DefaultControllerUtilities.filterAndAdjustVolumes(Arrays.asList(1d, 1d), 10_000, -1).isEmpty());
    }

    @Test
    public void testFilterAndAdjustVolumes_contractSize() {

        final List<Double> volumes = DefaultControllerUtilities.filterAndAdjustVolumes(Arrays.asList(1000d, 2000d), 10_000, -1);
        assertTrue(volumes.size() == 2);
        assertTrue(Double.compare(volumes.get(0), -0.1) == 0);
        assertTrue(Double.compare(volumes.get(1), -0.2) == 0);
    }

    @Test
    public void testFilterAndAdjustVolumes_rounding() {

        final List<Double> volumes = DefaultControllerUtilities.filterAndAdjustVolumes(Arrays.asList(1234d, 2345d), 1000, 1);
        assertTrue(volumes.size() == 2);
        assertTrue(Double.compare(volumes.get(0), 1.23) == 0);
        assertTrue(Double.compare(volumes.get(1), 2.34) == 0);
    }
}