package com.xm.jfund.controllers;

import com.xm.jfund.controllers.state.SplitVolumeContainer;
import com.xm.jfund.controllers.utilities.DefaultControllerUtilities;
import com.xm.jfund.math.MathUtils;
import com.xm.jfund.utils.StrategySymbolLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class DefaultController implements DecisionController {
    private static final Logger sLogger = LoggerFactory.getLogger(DefaultController.class);
    private final SplitVolumeContainer state = SplitVolumeContainer.create();
    //one exponent below the max. mantissa is 53 bits
    private final double MAX_DOUBLE_WE_ALLOW = Math.pow(2, 52);

    @Override
    public OrderVolumeCalculationResult calculateOrderVolume(
        final double sumOfExposures,
        final double netJfundExposure,
        final double symbolContractSize,
        final StrategySymbolLimits strategySymbolLimits) {

        final String symbol = strategySymbolLimits.getSymbolName();

        final int strategyId = strategySymbolLimits.getStrategyId();

        final double dangerExposureLevel = strategySymbolLimits.getDangerExposureLevel();
//        final double sumOfExposuresUsingDangerExposure = getSumOfExposuresUsingDangerExposureLevel(sumOfExposures, netJfundExposure, dangerExposureLevel);

        OrderVolumeCalculationResult result = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.NOT_TRADING, 0);
        if (doWeHaveAPreviousSplitVolumePending(state, symbol, strategyId)) {
            result = getPreviousSplitVolumeSavedInStateIfItsReady(symbol, strategyId);
            result = handlePendingVolume(symbolContractSize, result, sumOfExposures, symbol, strategyId, state);
        }
        else if (isThisSetOfExposuresLessThanOurSingleTradeLimit(sumOfExposures, strategySymbolLimits.getDangerTradeSizeLimit())) {
            result = splitVolumesAndGetTheFirstOneToExecute(strategySymbolLimits, symbol, sumOfExposures, symbolContractSize, strategyId, netJfundExposure, dangerExposureLevel);
        }
//        else if (isThisSetOfExposuresLessThanOurSingleTradeLimit(sumOfExposuresUsingDangerExposure, strategySymbolLimits.getDangerTradeSizeLimit())) {
//            sLogger.info("Using danger exposure level to check against single blocking trade value. {} {} {}", sumOfExposures, sumOfExposuresUsingDangerExposure, netJfundExposure);
//            result = splitVolumesAndGetTheFirstOneToExecute(strategySymbolLimits, symbol, sumOfExposuresUsingDangerExposure, symbolContractSize, strategyId, netJfundExposure, dangerExposureLevel);
//        }

        return result;
    }

    double getSumOfExposuresUsingDangerExposureLevel(final double originalSumOfExposures, final double netJfundExposure, final double dangerExposureLevel) {
        final double clientExposure = originalSumOfExposures - netJfundExposure;
        final double signMultiplier = MathUtils.getSignMultiplier(clientExposure);
        return signMultiplier * dangerExposureLevel + netJfundExposure;
    }

    /**
     * Ensure that the pending volume is still in the same direction we want to go. Also
     * take the anti of the sum of exposures if it's smaller than the pending volume.
     *
     * @param symbolContractSize        symbol contract size
     * @param pendingVolumeResult       initial pending volume result
     * @param sumOfExposures            current sum of exposures
     * @param symbol                    symbol
     * @param strategyId                strategy
     * @param splitVolumeContainerState split volume container
     * @return order volume calculation result
     */
    OrderVolumeCalculationResult handlePendingVolume(final double symbolContractSize,
                                                     final OrderVolumeCalculationResult pendingVolumeResult,
                                                     final double sumOfExposures,
                                                     final String symbol,
                                                     final int strategyId,
                                                     final SplitVolumeContainer splitVolumeContainerState) {
        OrderVolumeCalculationResult returnedResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.NOT_TRADING, 0);
        if (pendingVolumeResult.isValid()) {
            //this can happen on pending trades. The sum of exposures can drift.
            //we don't want to overshoot them with a previous pending trade
            final double orderVolume = pendingVolumeResult.getOrderVolume();
            final double antiVolumeToTrade = DefaultControllerUtilities.getAntiVolumeToTrade(sumOfExposures, symbolContractSize);
            //zero anti volume will be a direction change
            if (!isPendingVolumeInSameDirection(orderVolume, antiVolumeToTrade)) {
                splitVolumeContainerState.clear(strategyId, symbol);
                returnedResult = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.NOT_TRADING, 0);
                sLogger.info("Pending volume was not in same direction as our intended direction. Clearing state.  Strategy: {}, Symbol: {}, Sum of exposures: {}, Pending volume: {}", strategyId, symbol, sumOfExposures, orderVolume);
            }
            else if (isOverTheAntiVolume(orderVolume, antiVolumeToTrade)) {

                returnedResult = OrderVolumeCalculationResult.create(pendingVolumeResult.getOrderVolumeCalculationStatus(), antiVolumeToTrade);
                sLogger.info("Pending volume was larger than sum of exposures. Clearing state. Using sum of exposures instead. Clearing pending volume state. Strategy: {}, Symbol: {}, Sum of exposures: {}, Old volume: {}, New volume: {}", strategyId, symbol, sumOfExposures, orderVolume, antiVolumeToTrade);

                splitVolumeContainerState.clear(strategyId, symbol);
            }
            else {
                returnedResult = pendingVolumeResult;
            }
        }
        return returnedResult;
    }

    boolean isPendingVolumeInSameDirection(final double orderVolume, final double antiVolumeToTrade) {

        return Double.compare(MathUtils.getSignMultiplier(antiVolumeToTrade), MathUtils.getSignMultiplier(orderVolume)) == 0;
    }

    boolean isOverTheAntiVolume(final double orderVolume, final double antiVolume) {
        return Math.abs(orderVolume) > Math.abs(antiVolume);
    }

    /**
     * Split up the total volume into smaller volumes dictated by max instrument size
     * then retrieve the first volume to execute
     *
     * @param strategySymbolLimits symbol limits
     * @param symbol               symbol we care about
     * @param sumOfExposures       sum of exposures for the symbol we care about
     * @param symbolContractSize   contract size for said symbol
     * @param strategyId           id of strategy for this symbol
     * @return an order calculation result
     */
    private OrderVolumeCalculationResult splitVolumesAndGetTheFirstOneToExecute(final StrategySymbolLimits strategySymbolLimits,
                                                                                final String symbol,
                                                                                final double sumOfExposures,
                                                                                final double symbolContractSize,
                                                                                final int strategyId,
                                                                                final double netJfundExposure,
                                                                                final double dangerExposure) {

        validateDoubles(sumOfExposures, netJfundExposure);

        final double maxInstrumentsPerSingleTrade = strategySymbolLimits.getMaxInstrumentsPerSingleTrade();
        final long tradeDelayInMillis = strategySymbolLimits.getTradeDelayInMillis();
        final double orderVolume = getVolumeToTradeAndUpdateState(strategyId, symbol, sumOfExposures, symbolContractSize, maxInstrumentsPerSingleTrade, tradeDelayInMillis, netJfundExposure, dangerExposure);
        OrderVolumeCalculationStatus calculationStatus = OrderVolumeCalculationStatus.OK;
        if (MathUtils.isZero(orderVolume)) {
            calculationStatus = OrderVolumeCalculationStatus.NOT_TRADING;
        }
        return OrderVolumeCalculationResult.create(calculationStatus, orderVolume);
    }

    private void validateDoubles(final double sumOfExposures, final double netJfundExposure) {

        if (sumOfExposures >= MAX_DOUBLE_WE_ALLOW || netJfundExposure >= MAX_DOUBLE_WE_ALLOW) {
            throw new RuntimeException(String.format("values will lose precision. Sum of exposures: %f, Net JFund Exposures: %f", sumOfExposures, netJfundExposure));
        }
    }

    /**
     * Is the sum of exposures below our single trade limit?
     *
     * @param sumOfExposures         sum of exposures
     * @param dangerSingleTradeLimit dangerous single trade limit
     * @return true if it is
     */
    private boolean isThisSetOfExposuresLessThanOurSingleTradeLimit(final double sumOfExposures, final double dangerSingleTradeLimit) {
        return Math.abs(sumOfExposures) < dangerSingleTradeLimit;
    }

    /**
     * Do we have a previous split volume saved in out state that we still need to execute?
     *
     * @param container  that contains our state of split volumes
     * @param symbol     symbol we care about for trading
     * @param strategyId id of strategy related to the symbol
     * @return true if we have a saved split volume in our state
     */
    private boolean doWeHaveAPreviousSplitVolumePending(final SplitVolumeContainer container, final String symbol, final int strategyId) {
        return container.hasAVolume(strategyId, symbol);
    }

    /**
     * Get Previous saved split volume in our state if it's ready for execution
     *
     * @param symbol     symbol for volume we care about
     * @param strategyId id of strategy for this symbol
     * @return a volume if it's ready
     */
    private OrderVolumeCalculationResult getPreviousSplitVolumeSavedInStateIfItsReady(final String symbol, final int strategyId) {
        OrderVolumeCalculationResult result = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.NOT_TRADING, 0);
        final Optional<Double> volumeIfReady = state.getVolumeIfReady(strategyId, symbol);
        if (volumeIfReady.isPresent()) {
            result = OrderVolumeCalculationResult.create(OrderVolumeCalculationStatus.OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME, volumeIfReady.get());
        }
        return result;
    }

    @Override
    public boolean hasPendingTrades(final int strategyId, final String symbol) {
        return state.hasAVolume(strategyId, symbol);
    }

    /**
     * Get volume to trade and update the state of controller.
     * Since we split large volumes for certain trades, we need
     * to remember all the little volumes we have to execute.
     *
     * @param strategyId                   id for strategy
     * @param symbol                       symbol for volume
     * @param sumOfExposures               current sum of exposures
     * @param symbolContractSize           contract size for currency
     * @param maxInstrumentsPerSingleTrade maximum instruments for one single trade
     * @param tradeDelayInMillis           the amount of delay we want between trades for a particular strategy/currency pair
     * @return the first volume we should execute
     */
    private double getVolumeToTradeAndUpdateState(final int strategyId,
                                                  final String symbol,
                                                  final double sumOfExposures,
                                                  final double symbolContractSize,
                                                  final double maxInstrumentsPerSingleTrade,
                                                  final long tradeDelayInMillis,
                                                  final double netJfundExposure,
                                                  final double dangerExposure) {
        final double naivePotentialExposureToTrade = DefaultControllerUtilities.getOpposite(sumOfExposures);

        final double resultPotentialVolume = getVolumeThatDoesntExceedDanger(naivePotentialExposureToTrade, netJfundExposure, dangerExposure);

        final List<Double> volumes = getVolumes(maxInstrumentsPerSingleTrade, resultPotentialVolume, symbolContractSize);

        final double orderVolume;
        final int size = volumes.size();
        if (size > 0) {
            validateSplitVolumes(volumes, resultPotentialVolume);
            state.putVolumes(volumes, strategyId, symbol, tradeDelayInMillis);
            final Optional<Double> volume = state.getVolumeIfReady(strategyId, symbol);
            orderVolume = volume.orElse(0d);
        }
        else {
            orderVolume = 0;
        }

        return orderVolume;
    }

    List<Double> getVolumes(final double maxInstrumentsPerSingleTrade, final double resultPotentialVolume, final double symbolContractSize) {
        final List<Double> volumes;
        if (MathUtils.isZero(maxInstrumentsPerSingleTrade)) {
            volumes = new LinkedList<>();
            final double adjusted = DefaultControllerUtilities.roundDownAndApplyContractSize(resultPotentialVolume, symbolContractSize);
            if (!MathUtils.isZero(adjusted)) {
                volumes.add(adjusted);
            }
        }
        else {
            volumes = DefaultControllerUtilities.splitTotalInstrumentsIntoMaxVolumePerTrade(
                Math.abs(resultPotentialVolume),
                maxInstrumentsPerSingleTrade,
                symbolContractSize,
                MathUtils.getSignMultiplier(resultPotentialVolume));
        }
        return volumes;
    }

    double getVolumeThatDoesntExceedDanger(final double naivePotentialExposureToTrade, final double netJfundExposure, final double dangerExposure) {
        final double finalExposurePositive = Math.abs(naivePotentialExposureToTrade + netJfundExposure);
        final double resultPotentialVolume;
        if (finalExposurePositive > dangerExposure) {
            final double difference = finalExposurePositive - dangerExposure;
            resultPotentialVolume = MathUtils.getSignMultiplier(naivePotentialExposureToTrade) * (Math.abs(naivePotentialExposureToTrade) - difference);
        }
        else {
            resultPotentialVolume = naivePotentialExposureToTrade;
        }
        return resultPotentialVolume;
    }

    void validateSplitVolumes(final List<Double> volumes, final double naivePotentialExposureToTrade) {
        final double sum = volumes.stream().mapToDouble(x -> x).sum();
        final double signMultiplierOriginal = MathUtils.getSignMultiplier(naivePotentialExposureToTrade);

        final long count = volumes.stream().filter(v -> Double.compare(MathUtils.getSignMultiplier(v), signMultiplierOriginal) == 0).count();

        if (count != volumes.size()) {
            sLogger.error("Bad sign for split volumes. Naive volume: {}, Split volumes: {}", naivePotentialExposureToTrade, volumes);
            throw new RuntimeException("Did not get the sign correct for splitting volumes");
        }

        if (Math.abs(sum) > Math.abs(naivePotentialExposureToTrade)) {
            sLogger.error("Incorrect splits: Sum of splits: {}, Naive volue: {}, Splits: {}", sum, naivePotentialExposureToTrade, volumes);
            throw new RuntimeException("Calculated splits incorrectly.");
        }
    }
}
