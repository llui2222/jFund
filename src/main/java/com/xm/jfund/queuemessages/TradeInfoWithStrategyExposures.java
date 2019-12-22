/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xm.jfund.queuemessages;

import com.xm.jfund.trade.TradeObject;
import com.xm.jfund.utils.TradeExposureOnStrategy;

import java.util.List;

/**
 * @author nmichael
 */
public final class TradeInfoWithStrategyExposures implements ExposuresMessage {

    public static TradeInfoWithStrategyExposures create(
        final List<TradeExposureOnStrategy> tradeExposureOnStrategies,
        final TradeObject trade,
        final int serverId) {

        return new TradeInfoWithStrategyExposures(tradeExposureOnStrategies, trade, serverId);
    }

    @Override
    public void accept(final ExposuresVisitor visitor) {

        visitor.visit(this);
    }

    public TradeObject getTrade() {
        return mTrade;
    }

    public List<TradeExposureOnStrategy> getTradeExposureOnStrategies() {
        return mTradeExposureOnStrategies;
    }

    public int getServerId() {
        return mServerId;
    }

    private TradeInfoWithStrategyExposures(
        final List<TradeExposureOnStrategy> tradeExposureOnStrategies,
        final TradeObject trade,
        final int serverId) {

        mTrade = trade;
        mTradeExposureOnStrategies = tradeExposureOnStrategies;
        mServerId = serverId;
    }

    private final TradeObject mTrade;
    private final List<TradeExposureOnStrategy> mTradeExposureOnStrategies;
    private final int mServerId;
}
