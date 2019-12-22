package com.xm.jfund.queuemessages;

public class TradeServiceError implements TradeServiceMessage {

    private final Throwable error;

    private TradeServiceError(final Throwable error) {
        this.error = error;
    }

    public static TradeServiceError create(final Throwable error) {
        return new TradeServiceError(error);
    }

    @Override
    public void accept(final TradeServiceVisitor visitor) {
        visitor.visit(this);
    }

    public Throwable getError() {
        return error;
    }
}
