/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xm.jfund.queuemessages;

import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.client.trade.model.Trade;
import com.xm.jfund.trade.TradeExposureFactory;
import com.xm.jfund.trade.TradeObject;
import com.xm.jfund.trade.TradeObjectFactory;
import com.xm.jfund.trade.TradeObjectType;
import com.xm.jfund.utils.TradeExposureOnStrategy;
import jAnalystUtils.TradeExposure;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ExposuresMessageFactory {

    public static ExposuresMessage create(final List<Integer> strategies, final Trade trade) {

        final TradeExposure tradeExposure = TradeExposureFactory.create(trade);
        final List<TradeExposureOnStrategy> exposureOnStrategies = strategies.stream()
            .map(strategyId -> TradeExposureOnStrategy.create(strategyId, tradeExposure, getTradingAccountRiskGroupName(strategyId), false))
            .collect(Collectors.toList());

        final TradeObject tradeObject = TradeObjectFactory.create(trade, TradeObjectType.OPEN_TRADE);

        return TradeInfoWithStrategyExposures.create(exposureOnStrategies, tradeObject, 0);
    }

    public static ExposuresMessage create(final int strategyId, final Exposure exposure) {

        final TradeExposure tradeExposure = TradeExposureFactory.create(exposure);
        final TradeExposureOnStrategy expOnStrategy = TradeExposureOnStrategy.create(strategyId, tradeExposure, getTradingAccountRiskGroupName(strategyId), false);
        return TradeInfoWithStrategyExposures.create(Collections.singletonList(expOnStrategy), TradeObjectFactory.create(exposure, TradeObjectType.OPEN_TRADE), 0);
    }

    private static String getTradingAccountRiskGroupName(final int strategyId) {

        return String.format("AntiCoverage_%d", strategyId);
    }
}
