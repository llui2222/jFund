package com.xm.jfund.controllers;

import com.xm.jfund.controllers.state.SplitVolumeContainer;
import com.xm.jfund.utils.StrategySymbolLimits;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultControllerTest {
    private static final String sSymbol = "EURUSD";
    private static final double sSymbolContractSize = 1;

    private final DefaultController controller = new DefaultController();
    private StrategySymbolLimits strategySymbolLimits;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        strategySymbolLimits = mock(StrategySymbolLimits.class);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(50.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(100.0);
        when(strategySymbolLimits.getStrategyId()).thenReturn(1);
        when(strategySymbolLimits.getSymbolName()).thenReturn(sSymbol);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(5.0);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(10.0);

        final double netJfundExposureDoesntMatter = 100;
        final double sumOfExposures = 20.1;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposureDoesntMatter, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0.0);
    }

//    @Test
//    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_butNotWhileUsingDangerLevel() {
//        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
//        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
//        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);
//
//        final double netJFundExposure = 4_000_000;
//        final double sumOfExposures = -9_000_000;
//        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);
//
//        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
//        assertThat(result.getOrderVolume()).isEqualTo(1_000_000);
//    }

//    @Test
//    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_largeClient0() {
//        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
//        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
//        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);
//
//        final double netJFundExposure = 4_000_000;
//        final double sumOfExposures = -40_000_000;
//        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);
//
//        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
//        assertThat(result.getOrderVolume()).isEqualTo(1_000_000);
//    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_close() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = -999_999;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(999_999);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_close1() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = -1_000_000;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(1_000_000);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_close2() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = -1_000_000.9080248029348;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(1_000_000);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_close3() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = -1_000_000.9999999999999999997;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(1_000_000);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_atSingleTradeDanger() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 2_000_000;
        final double sumOfExposures = -2_000_000;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_atSingleTradeDangerClose() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 2_000_000;
        final double sumOfExposures = -1_999_999.999999999999999999;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, 100_000, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_atSingleTradeDangerClose1() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 2_000_000;
        final double sumOfExposures = -1_999_999.5;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, 100_000, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(19.99);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_atSingleTradeDangerClose2() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 2_000_000;
        final double sumOfExposures = -2_000_000.0001;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, 100_000, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0);
    }


    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_butNotWhileUsingDangerLevel_largeClientExposure() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000D);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = 20_000_000;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0.0);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_butNotWhileUsingDangerLevel_equalClientExposure() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000D);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = 4_000_000;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0.0);
    }

    @Test
    public void testCalculateOrderVolumeSingleTradeThresholdExceeded_butNotWhileUsingDangerLevel_zeroSumOfExposures() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000D);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000d);

        final double netJFundExposure = 4_000_000;
        final double sumOfExposures = 0;
        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJFundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0.0);
    }

    @Test
    public void testCalculateOrderVolumeDangerLevelThresholdReached() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(20.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(80.0);

        final double sumOfExposures = -20;
        final double netJfundExposure = 80.0;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0);
    }

    /**
     * We round to zero, so we can't trade
     */
    @Test
    public void testCalculateOrderVolumeDangerLevelThresholdAlmostReached() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(10.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(80.0);
        final double netJfundExposure = 79.999;
        final double sumOfExposures = -20.00;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0);
    }

    @Test
    public void testProd() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(350_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(6_000_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(2_750_000d);
        final double netJfundExposure = 5650000;
        final double sumOfExposures = 3848024.159999908;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(result.getOrderVolume()).isEqualTo(0);
    }

    @Test
    public void testCalculateOrderVolumeDangerLevelThresholdExceeded() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(20.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(90.0);

        final double sumOfExposures = 20;
        final double netJfundExposure = -80;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(-10.0);
    }

    @Test
    public void testCalculateOrderVolumeSumOfExposures() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(20.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(150.0);

        final double sumOfExposures = 20.0;
        final double netJfundExposure = 100;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(-20.0);
    }

    @Test
    public void testSplitVolumes() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(20.0);
        when(strategySymbolLimits.getMaxInstrumentsPerSingleTrade()).thenReturn(50.0);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(101.0);

        final double sumOfExposures = 100;
        final double netJfundExposure = 0;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK);
        assertThat(result.getOrderVolume()).isEqualTo(-50.0);

        assertThat(controller.hasPendingTrades(strategySymbolLimits.getStrategyId(), strategySymbolLimits.getSymbolName())).isEqualTo(true);

        final double exposuresDontMatterAnyMore = 100000;
        final double currentJfundCoverage = netJfundExposure + result.getOrderVolume();

        final OrderVolumeCalculationResult nextResult = controller.calculateOrderVolume(exposuresDontMatterAnyMore, currentJfundCoverage, sSymbolContractSize, strategySymbolLimits);

        assertThat(nextResult.getOrderVolumeCalculationStatus()).isEqualTo(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME);
        assertThat(nextResult.getOrderVolume()).isEqualTo(-50.0);

        assertThat(controller.hasPendingTrades(strategySymbolLimits.getStrategyId(), strategySymbolLimits.getSymbolName())).isEqualTo(false);
    }

    @Test
    public void testSplitVolumesAdjustForDangerLevel() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(20.0);
        when(strategySymbolLimits.getMaxInstrumentsPerSingleTrade()).thenReturn(30.0);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(80.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(50.0);

        final double sumOfExposures = 60;
        final double netJfundExposure = 0;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, sSymbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolume()).isEqualTo(-30.0);

        final double exposuresDontMatterAnyMore = 100000;
        final double currentJfundCoverage = netJfundExposure + result.getOrderVolume();

        final OrderVolumeCalculationResult nextResult = controller.calculateOrderVolume(exposuresDontMatterAnyMore, currentJfundCoverage, sSymbolContractSize, strategySymbolLimits);

        assertThat(nextResult.getOrderVolume()).isEqualTo(-20.0);
    }

    @Test
    public void testSplitVolumesAdjustForChangingSumOfExposures() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(100d);
        when(strategySymbolLimits.getMaxInstrumentsPerSingleTrade()).thenReturn(300_000d);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(1_000_000d);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(1_000_000d);

        final double netJfundExposure = 0;

        final double symbolContractSize = 100_000;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(400_000, netJfundExposure, symbolContractSize, strategySymbolLimits);

        assertThat(result.getOrderVolume()).isEqualTo(-3);

        final double currentJfundCoverage = netJfundExposure + result.getOrderVolume() * symbolContractSize;

        final OrderVolumeCalculationResult nextResult = controller.calculateOrderVolume(50_000, currentJfundCoverage, symbolContractSize, strategySymbolLimits);

        assertThat(nextResult.getOrderVolume()).isEqualTo(-0.5);
    }

    /*
    Contrived example to ensure we don't trade volumes of size 0
     */
    @Test
    public void testSplitsToZeroVolumes() {
        when(strategySymbolLimits.getTradingThreshold()).thenReturn(200_000.0);
        when(strategySymbolLimits.getMaxInstrumentsPerSingleTrade()).thenReturn(20_000.0);
        when(strategySymbolLimits.getDangerExposureLevel()).thenReturn(5_000_000.0);
        when(strategySymbolLimits.getDangerTradeSizeLimit()).thenReturn(500_000.0);

        final double sumOfExposures = 200_000.1;
        final double netJfundExposure = 0;

        final OrderVolumeCalculationResult result = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, 100_000_000, strategySymbolLimits);

        assertThat(result.getOrderVolume()).isEqualTo(0);
        assertThat(result.getOrderVolumeCalculationStatus() == OrderVolumeCalculationStatus.NOT_TRADING);
        assertThat(controller.hasPendingTrades(strategySymbolLimits.getStrategyId(), strategySymbolLimits.getSymbolName())).isFalse();
    }

    @Test
    public void testFlow() {
        final int strat1 = 1;
        final String symbol = "EURUSD";
        final double contractSize = 100_000;

        final StrategySymbolLimits strategySymbolLimits = getStrategySymbolLimits(
            strat1,
            symbol
        );

        final double sumOfExposures = -500_000;
        final double netJfundExposure = 4_500_000;

        final OrderVolumeCalculationResult firstResult = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, contractSize, strategySymbolLimits);

        final double firstVolume = firstResult.getOrderVolume();
        assertThat(firstVolume).isEqualTo(0.1);
        assertThat(firstResult.isValid()).isEqualTo(true);

        final double secondSumOfExposures = 5_000;
        final double secondNetJfundExposure = netJfundExposure + firstVolume * contractSize; //4_510_000

        final OrderVolumeCalculationResult secondResult = controller.calculateOrderVolume(secondSumOfExposures, secondNetJfundExposure, contractSize, strategySymbolLimits);

        final double secondVolume = secondResult.getOrderVolume();
        assertThat(secondVolume).isEqualTo(0);
        assertThat(secondResult.isValid()).isEqualTo(false);

        final double thirdSumOfExposures = 40_000_000;
        final double thirdNetJfundExposure = secondNetJfundExposure + secondVolume * contractSize;

        final OrderVolumeCalculationResult thirdResult = controller.calculateOrderVolume(thirdSumOfExposures, thirdNetJfundExposure, contractSize, strategySymbolLimits);

        final double thirdVolume = thirdResult.getOrderVolume();
        assertThat(thirdVolume).isEqualTo(0);
        assertThat(thirdResult.isValid()).isEqualTo(false);

        final double forthSumOfExposures = -749_000;
        final double forthNetJfundExposure = thirdNetJfundExposure + thirdVolume * contractSize;

        final OrderVolumeCalculationResult forthResult = controller.calculateOrderVolume(forthSumOfExposures, forthNetJfundExposure, contractSize, strategySymbolLimits);

        final double forthVolume = forthResult.getOrderVolume();
        assertThat(forthVolume).isEqualTo(0.1);
        assertThat(forthResult.isValid()).isEqualTo(true);

        final double fifthSumOfExposures = 0;
        final double fifthNetJfundExposure = forthNetJfundExposure + forthVolume * contractSize;

        final OrderVolumeCalculationResult fifthResult = controller.calculateOrderVolume(fifthSumOfExposures, fifthNetJfundExposure, contractSize, strategySymbolLimits);
        final double fifthVolume = fifthResult.getOrderVolume();
        assertThat(fifthVolume).isEqualTo(0.0);
        assertThat(fifthResult.isValid()).isEqualTo(false);

        final double sixthSumOfExposures = -600_000;
        final double sixthNetJfundExposure = fifthNetJfundExposure + fifthVolume * contractSize; //4_500_000

        final OrderVolumeCalculationResult sixthResult = controller.calculateOrderVolume(sixthSumOfExposures, sixthNetJfundExposure, contractSize, strategySymbolLimits);
        final double sixthVolume = sixthResult.getOrderVolume();
        assertThat(sixthVolume).isEqualTo(0.1);
        assertThat(sixthResult.isValid()).isEqualTo(true);

        final double seventhSumOfExposures = 0;
        final double seventhNetJfundExposure = sixthNetJfundExposure + sixthVolume * contractSize; //4_600_000

        final OrderVolumeCalculationResult seventhResult = controller.calculateOrderVolume(seventhSumOfExposures, seventhNetJfundExposure, contractSize, strategySymbolLimits);
        final double seventhVolume = seventhResult.getOrderVolume();
        assertThat(seventhVolume).isEqualTo(0);
        assertThat(seventhResult.isValid()).isEqualTo(false);
    }

    @Test
    public void testFlow_overDanger() {
        final int strat1 = 1;
        final String symbol = "EURUSD";
        final double contractSize = 100_000;

        final StrategySymbolLimits strategySymbolLimits = getStrategySymbolLimits(strat1, symbol);

        final double sumOfExposures = -500_000;
        final double netJfundExposure = 6_000_000;

        final OrderVolumeCalculationResult firstResult = controller.calculateOrderVolume(sumOfExposures, netJfundExposure, contractSize, strategySymbolLimits);

        final double firstVolume = firstResult.getOrderVolume();
        assertThat(firstVolume).isEqualTo(-0.1);
    }

    private StrategySymbolLimits getStrategySymbolLimits(final int strategyId,
                                                         final String symbolName) {
        return StrategySymbolLimits.create(
            strategyId,
            0,
            symbolName,
            200_000,
            0,
            5_000_000,
            0,
            750_000,
            0,
            15,
            10_000,
            0);
    }

    private OrderVolumeCalculationResult getOrderResult(final double orderVolume) {
        return OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK, orderVolume);
    }

    @Test
    public void testValidateSplitVolumes_signDoesntMatch() {

        final List<Double> doubles = Arrays.asList(1.4, -1.4, 2.8);
        final Double reduced = doubles.stream().map(Math::abs).reduce(0d, (old, newValue) -> old + newValue);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Did not get the sign correct for splitting volumes");

        controller.validateSplitVolumes(doubles, reduced);
    }

    @Test
    public void testValidateSplitVolumes_sumIsTooLarge() {

        final List<Double> doubles = Arrays.asList(-1.4, -1.4, -1.4);
        final Double sum = doubles.stream().mapToDouble(Double::doubleValue).sum();
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Calculated splits incorrectly.");
        controller.validateSplitVolumes(doubles, sum / 2);
    }

    @Test
    public void testGetSafeVolume_overDangerExposureLevel() {

        final double safeVolume = controller.getVolumeThatDoesntExceedDanger(-1_000_000, -2_000_000, 2_500_000);
        assertTrue(Double.compare(safeVolume, -500_000) == 0);
    }

    @Test
    public void testGetSafeVolume_overDangerExposureLevelPositive() {

        final double safeVolume = controller.getVolumeThatDoesntExceedDanger(100_000_000, -2_000_000, 95_000_001);
        assertTrue(Double.compare(safeVolume, 97_000_001) == 0);
    }

    @Test
    public void testGetSafeVolume_zero() {

        final double safeVolume = controller.getVolumeThatDoesntExceedDanger(-0, -2_000_000, 95_000_001);
        assertTrue(Double.compare(safeVolume, 0) == 0);
    }

    @Test
    public void testGetSafeVolume_zeroExposure() {

        final double safeVolume = controller.getVolumeThatDoesntExceedDanger(-95_000_002, -0, 95_000_001);
        assertTrue(Double.compare(safeVolume, -95_000_001) == 0);
    }

    @Test
    public void testGetSafeVolume_zeroDanger() {

        final double safeVolume = controller.getVolumeThatDoesntExceedDanger(-95_000_002, 100, 0);
        assertTrue(Double.compare(safeVolume, -100) == 0);
    }

    @Test
    public void testGetSafeVolume_swing() {

        final double safeVolume = controller.getVolumeThatDoesntExceedDanger(-95_000_000, 5_000_000, 6_000_000);
        assertTrue(Double.compare(safeVolume, -11_000_000) == 0);
    }

    @Test
    public void testGetVolumes_zeroMaxInstruments() {
        final List<Double> volumes = controller.getVolumes(0, -1, 1);
        assertTrue(volumes.size() == 1);
        assertTrue(Double.compare(volumes.get(0), -1) == 0);
    }

    @Test
    public void testIsPendingVolumeInSameDirection() {
        assertTrue(controller.isPendingVolumeInSameDirection(-0.0, +0.0));
        assertFalse(controller.isPendingVolumeInSameDirection(0.0, -1.0));
        assertFalse(controller.isPendingVolumeInSameDirection(+0.0, +1.0));
        assertFalse(controller.isPendingVolumeInSameDirection(-1.0, -0.0));
        assertFalse(controller.isPendingVolumeInSameDirection(-1.0, 1.0));
        assertTrue(controller.isPendingVolumeInSameDirection(-1.0, -1.0000000));
        assertFalse(controller.isPendingVolumeInSameDirection(1.0, 0.0));
        assertFalse(controller.isPendingVolumeInSameDirection(+1.0, -1.0));
        assertTrue(controller.isPendingVolumeInSameDirection(1.0, +1.0));
    }

    @Test
    public void testIsOverTheAntiVolume() {
        assertTrue(controller.isOverTheAntiVolume(1.0, -0));
        assertFalse(controller.isOverTheAntiVolume(1.0, -2));
        assertTrue(controller.isOverTheAntiVolume(1.0, 0.9));

        assertFalse(controller.isOverTheAntiVolume(-0.0, 1));
        assertFalse(controller.isOverTheAntiVolume(-0.0, 0));
        assertFalse(controller.isOverTheAntiVolume(0.0, -1));

        assertFalse(controller.isOverTheAntiVolume(-10.0, -20.0));
        assertFalse(controller.isOverTheAntiVolume(-10.0, 20.0));
        assertTrue(controller.isOverTheAntiVolume(-10.1, 10.0));
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_sumOfExposuresChangedToZero() {
        final double symbolContractSize = 100_000;
        final double pendingVolume = 1.0;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, pendingVolume);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);
        assertTrue(sampleContainer.hasAVolume(strategy, symbol));
        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, 0, symbol, strategy, sampleContainer);
        assertFalse(result.isValid());
        assertFalse(sampleContainer.hasAVolume(strategy, symbol));
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_sumOfExposuresChangedDirection() {
        final double symbolContractSize = 100_000;
        final double pendingVolume = 1.0;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, pendingVolume);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);

        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, 100, symbol, strategy, sampleContainer);
        assertFalse(result.isValid());
        assertFalse(sampleContainer.hasAVolume(strategy, symbol));
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_sumOfExposuresGreaterThanVolume() {
        final double symbolContractSize = 100_000;
        final double pendingVolume = 1.0;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, pendingVolume);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);

        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, -100_001, symbol, strategy, sampleContainer);
        assertTrue(result.isValid());
        assertTrue(sampleContainer.hasAVolume(strategy, symbol));
        assertTrue(Double.compare(result.getOrderVolume(), pendingVolume) == 0);
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_sumOfExposuresLessThanVolume() {
        final double symbolContractSize = 100_000;
        final double pendingVolume = 1.0;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, pendingVolume);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);

        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, -99_999, symbol, strategy, sampleContainer);
        assertTrue(result.isValid());
        assertFalse(sampleContainer.hasAVolume(strategy, symbol));
        assertTrue(Double.compare(result.getOrderVolume(), 0.99) == 0);
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_sumOfExposuresLessThanVolumeButTurnsIntoZero() {
        final double symbolContractSize = 100_000;
        final double pendingVolume = 1.0;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, pendingVolume);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);

        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, -1, symbol, strategy, sampleContainer);
        assertFalse(result.isValid());
        assertFalse(sampleContainer.hasAVolume(strategy, symbol));
        assertTrue(Double.compare(result.getOrderVolume(), 0) == 0);
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_sumOfExposuresSameAsVolume() {
        final double symbolContractSize = 100_000;
        final double pendingVolume = 1.0;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, pendingVolume);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);

        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, -100_000, symbol, strategy, sampleContainer);
        assertTrue(result.isValid());
        assertTrue(sampleContainer.hasAVolume(strategy, symbol));
        assertTrue(Double.compare(result.getOrderVolume(), 1.0) == 0);
    }

    @Test
    public void testHandleIfPendingVolumeIsStillValid_pendingVolumeResultNotValid() {
        final double symbolContractSize = 100_000;
        final String symbol = "USDJPY";
        final int strategy = 1;
        final OrderVolumeCalculationResult pendingVolumeResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.NOT_TRADING, 0);

        final SplitVolumeContainer sampleContainer = getSampleContainer(strategy, symbol);

        final OrderVolumeCalculationResult result = controller.handlePendingVolume(symbolContractSize, pendingVolumeResult, -100_000, symbol, strategy, sampleContainer);
        assertFalse(result.isValid());
        assertTrue(sampleContainer.hasAVolume(strategy, symbol));
        assertTrue(Double.compare(result.getOrderVolume(), 0) == 0);
    }

    @Test
    public void testGetSumOfExposuresusingDangerExposureLevel_zeros() {
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(0, 0, 0), 0) == 0);
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(-0, -0, -0), 0) == 0);
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(+0, -0, 0), 0) == 0);
    }

    @Test
    public void testGetSumOfExposuresusingDangerExposureLevel_clientExposureLargerThanDanger() {
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(-9_000_000, 4_000_000, 5_000_000), -1_000_000) == 0);
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(0, 4_000_000, 5_000_000), -1_000_000) == 0);
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(9_000_000, 4_000_000, 5_000_000), 9_000_000) == 0);
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(20_000_000, 4_000_000, 5_000_000), 9_000_000) == 0);
        assertTrue(Double.compare(controller.getSumOfExposuresUsingDangerExposureLevel(-20_000_000, 4_000_000, 5_000_000), -1_000_000) == 0);
    }

    private SplitVolumeContainer getSampleContainer(final int strategy, final String symbol) {
        final SplitVolumeContainer splitVolumeContainer = SplitVolumeContainer.create();
        final List<Double> volumes = Arrays.asList(1.0, 1.0, 1.0);
        splitVolumeContainer.putVolumes(volumes, strategy, symbol, 0);
        return splitVolumeContainer;
    }
}