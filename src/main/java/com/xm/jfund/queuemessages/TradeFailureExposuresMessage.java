package com.xm.jfund.queuemessages;

public final class TradeFailureExposuresMessage implements ExposuresMessage {

    public static TradeFailureExposuresMessage create(
        final String failedTradeRequestComment, final int strategyId, final String symbol) {
        return new TradeFailureExposuresMessage(failedTradeRequestComment, strategyId, symbol);
    }

    @Override
    public void accept(final ExposuresVisitor visitor) {

        visitor.visit(this);
    }

    public String getFailedTradeRequestComment() {
        return mFailedTradeRequestComment;
    }

    public int getStrategyId() {
        return mStrategyId;
    }

    public String getSymbol() {
        return mSymbol;
    }

    private TradeFailureExposuresMessage(
        final String failedTradeRequestComment, final int strategyId, final String symbol) {
        mFailedTradeRequestComment = failedTradeRequestComment;
        mStrategyId = strategyId;
        mSymbol = symbol;
    }

    private final String mFailedTradeRequestComment;
    private final int mStrategyId;
    private final String mSymbol;
}
