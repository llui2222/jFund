package com.xm.jfund.controllers.state;

import com.xm.jfund.utils.StrategySymbolKey;
import net.jcip.annotations.NotThreadSafe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

/**
 * A container of a large volume split into several volumes
 * The volumes are related to a strategy/symbol pair
 */
@NotThreadSafe
public final class SplitVolumeContainer {

    private final Map<StrategySymbolKey, Queue<Double>> state;

    public static SplitVolumeContainer create() {
        return new SplitVolumeContainer(new HashMap<>());
    }

    private SplitVolumeContainer(final Map<StrategySymbolKey, Queue<Double>> map) {
        this.state = map;
    }

    /**
     * Does the container have a volume for this particular
     * strategy and symbol?
     *
     * @param strategyId strategy for volume
     * @param symbol     symbol for volume
     * @return true if there is some volume for this strategy/symbol pair
     */
    public boolean hasAVolume(final int strategyId, final String symbol) {
        final StrategySymbolKey key = getCompositeKey(strategyId, symbol);

        final Queue<Double> volume = state.get(key);

        return volume != null && volume.size() > 0;
    }

    /**
     * Put a series of volumes for this strategy/symbol pair, replacing olds ones if they existed
     *
     * @param volumes       volumes to add
     * @param strategyId    strategy id
     * @param symbol        symbol
     * @param delayInMillis delay in milliseconds between successive trades
     */
    public void putVolumes(final List<Double> volumes, final int strategyId, final String symbol, final long delayInMillis) {

        final StrategySymbolKey key = getCompositeKey(strategyId, symbol);

        final Queue<Double> result = new TimedQueue<>(delayInMillis);
        result.addAll(volumes);
        state.put(key, result);
    }

    /**
     * Get volume if it's ready to execute.
     * By ready, we mean has the appropriate time elapsed?
     *
     * @param strategyId id of strategy
     * @param symbol     symbol
     * @return an optional volume if it's ready
     */
    public Optional<Double> getVolumeIfReady(final int strategyId, final String symbol) {
        final StrategySymbolKey key = getCompositeKey(strategyId, symbol);
        Optional<Double> potentialVolume = Optional.empty();

        if (hasAVolume(strategyId, symbol)) {
            final Queue<Double> volumes = state.get(key);
            final Double first = volumes.poll();
            if (first != null) {
                potentialVolume = Optional.of(first);
            }
        }

        return potentialVolume;
    }

    /**
     * Get key composed of strategy id and symbol
     *
     * @param strategyId strategy id
     * @param symbol     symbol
     * @return a composite key made up of the parameters given
     */
    private StrategySymbolKey getCompositeKey(final int strategyId, final String symbol) {
        return StrategySymbolKey.create(strategyId, symbol);
    }

    public void clear(final int strategyId, final String symbol) {
        state.remove(getCompositeKey(strategyId, symbol));
    }
}
