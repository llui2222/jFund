package com.xm.jfund.queuemessages;

public interface TradeServiceMessage {

    void accept(final TradeServiceVisitor visitor);
}
