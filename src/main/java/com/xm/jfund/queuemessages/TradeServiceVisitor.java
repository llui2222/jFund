package com.xm.jfund.queuemessages;

public interface TradeServiceVisitor {

    void visit(final TradeServiceConnectionEstablished msg);

    void visit(final TradeServicePosition msg);

    void visit(final TradeServiceError msg);
}
