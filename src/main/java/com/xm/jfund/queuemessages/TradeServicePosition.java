package com.xm.jfund.queuemessages;

import com.xm.jfund.client.trade.model.Trade;

public class TradeServicePosition implements TradeServiceMessage {

    private final Trade trade;

    private TradeServicePosition(final Trade trade) {
        this.trade = trade;
    }

    public static TradeServicePosition create(final Trade trade) {
        return new TradeServicePosition(trade);
    }

    @Override
    public void accept(final TradeServiceVisitor visitor) {
        visitor.visit(this);
    }

    public Trade getTrade() {
        return trade;
    }
}
