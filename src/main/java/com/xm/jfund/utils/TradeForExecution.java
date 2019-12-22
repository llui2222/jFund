/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xm.jfund.utils;

import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;

import java.math.BigDecimal;

/**
 * @author nmichael
 */
public final class TradeForExecution {

    private final int mStrategyId;
    private final String mSymbol;
    private final BigDecimal mVolume;
    private final String mComment;
    private final Side side;
    private final TYPE tradeStrategySymbolLimitsExecutionType;

    private TradeForExecution(final int strategyId, final String symbol, final BigDecimal volume, final String comment, final Side side, final TYPE type) {
        mStrategyId = strategyId;
        mSymbol = symbol;
        mVolume = volume;
        mComment = comment;
        this.side = side;
        this.tradeStrategySymbolLimitsExecutionType = type;
    }

    public static TradeForExecution create(final int strategyId, final String symbol, final BigDecimal volume, final String comment, final Side side, final TYPE type) {
        return new TradeForExecution(strategyId, symbol, volume, comment, side, type);
    }

    public TYPE getTradeStrategySymbolLimitsExecutionType() {
        return tradeStrategySymbolLimitsExecutionType;
    }

    public int getStrategyId() {
        return mStrategyId;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public BigDecimal getVolume() {
        return mVolume;
    }

    public String getComment() {
        return mComment;
    }

    public Side getSide() {
        return side;
    }
}
