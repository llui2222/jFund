package com.xm.jfund.queuemessages;

public interface ExposuresVisitor {
    void visit(final TradeInfoWithStrategyExposures t);
    void visit(final TradeFailureExposuresMessage t);
}
