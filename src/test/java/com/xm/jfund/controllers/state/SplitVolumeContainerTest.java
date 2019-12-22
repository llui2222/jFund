package com.xm.jfund.controllers.state;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SplitVolumeContainerTest {

    @Test
    public void hasAVolumeTest() {
        final SplitVolumeContainer container = SplitVolumeContainer.create();
        final List<Double> someVolumes = getSomeVolumes();

        final int strategyIdOne = 1;
        final String symbolOne = "EURUSD";
        final long delayInMillis = 2_000;

        container.putVolumes(someVolumes, strategyIdOne, symbolOne, delayInMillis);

        assertTrue(container.hasAVolume(strategyIdOne, symbolOne));
        assertFalse(container.hasAVolume(strategyIdOne + 1, symbolOne));
    }

    @Test
    public void hasAVolumeAfterExhaustingVolumesTest() {
        final SplitVolumeContainer container = SplitVolumeContainer.create();
        final List<Double> someVolumes = getSomeVolumes();

        final int strategyIdOne = 1;
        final String symbolOne = "EURUSD";
        final long delayInMillis = 1_000 * 60;

        container.putVolumes(someVolumes, strategyIdOne, symbolOne, delayInMillis);

        Optional<Double> volumeIfReady = container.getVolumeIfReady(strategyIdOne, symbolOne);

        assertTrue(volumeIfReady.isPresent());
        assertTrue(Double.compare(volumeIfReady.get(), 10.0) == 0);
        assertFalse(container.hasAVolume(strategyIdOne, symbolOne));

        container.putVolumes(someVolumes, strategyIdOne, symbolOne, delayInMillis);

        volumeIfReady = container.getVolumeIfReady(strategyIdOne, symbolOne);

        assertTrue(volumeIfReady.isPresent());
        assertTrue(Double.compare(volumeIfReady.get(), 10.0) == 0);
        assertFalse(container.hasAVolume(strategyIdOne, symbolOne));
    }

    @Test
    public void testTwoDifferentStrategies() {
        final SplitVolumeContainer container = SplitVolumeContainer.create();
        final List<Double> someVolumesOne = getSomeVolumes();
        final List<Double> someVolumesTwo = getSomeVolumes();

        final int strategyIdOne = 1;
        final String symbolOne = "EURUSD";

        final int strategyIdTwo = 2;
        final String symbolTwo = "EURUSD";

        container.putVolumes(someVolumesOne, strategyIdOne, symbolOne, 2_000);
        container.putVolumes(someVolumesTwo, strategyIdTwo, symbolTwo, 1_000);

        container.getVolumeIfReady(strategyIdOne, symbolOne);

        assertFalse(container.hasAVolume(strategyIdOne, symbolOne));
        assertTrue(container.hasAVolume(strategyIdTwo, symbolTwo));
    }

    @Test
    public void testTwoDifferentCurrencies() {
        final SplitVolumeContainer container = SplitVolumeContainer.create();
        final List<Double> someVolumesOne = getSomeVolumes();
        final List<Double> someVolumesTwo = getSomeMoreVolumes();

        final int strategyIdOne = 1;
        final String symbolOne = "EURUSD";

        final String symbolTwo = "JPYUSD";

        container.putVolumes(someVolumesOne, strategyIdOne, symbolOne, 2_000);
        container.putVolumes(someVolumesTwo, strategyIdOne, symbolTwo, 2_000);

        final Optional<Double> volumeOne = container.getVolumeIfReady(strategyIdOne, symbolOne);
        final Optional<Double> volumeTwo = container.getVolumeIfReady(strategyIdOne, symbolTwo);

        assertTrue(volumeOne.isPresent());
        assertTrue(volumeTwo.isPresent());
        assertTrue(Double.compare(volumeOne.get(), 10.0) == 0);
        assertTrue(Double.compare(volumeTwo.get(), 11.0) == 0);
    }

    @Test
    public void testVolumeIsNotReady() {
        final SplitVolumeContainer container = SplitVolumeContainer.create();
        final List<Double> someVolumesOne = getSomeMoreVolumes();

        final int strategyIdOne = 1;
        final String symbolOne = "EURUSD";

        container.putVolumes(someVolumesOne, strategyIdOne, symbolOne, 60_000);

        final Optional<Double> volumeOne = container.getVolumeIfReady(strategyIdOne, symbolOne);

        assertTrue(volumeOne.isPresent());

        final Optional<Double> volumeTwo = container.getVolumeIfReady(strategyIdOne, symbolOne);

        assertFalse(volumeTwo.isPresent());
        assertTrue(container.hasAVolume(strategyIdOne, symbolOne));
    }

    private List<Double> getSomeVolumes() {
        final List<Double> volumes = new LinkedList<>();
        volumes.add(10.0);

        return volumes;
    }

    private List<Double> getSomeMoreVolumes() {
        final List<Double> volumes = new LinkedList<>();
        volumes.add(11.0);
        volumes.add(2.0);
        return volumes;
    }
}