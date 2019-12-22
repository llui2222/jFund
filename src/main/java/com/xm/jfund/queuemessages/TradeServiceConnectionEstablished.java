package com.xm.jfund.queuemessages;

import org.springframework.messaging.simp.stomp.StompSession;

public class TradeServiceConnectionEstablished implements TradeServiceMessage {

    private final StompSession session;

    private TradeServiceConnectionEstablished(final StompSession session) {
        this.session = session;
    }

    public static TradeServiceConnectionEstablished create(final StompSession session) {
        return new TradeServiceConnectionEstablished(session);
    }

    @Override
    public void accept(final TradeServiceVisitor visitor) {
        visitor.visit(this);
    }

    public StompSession getSession() {
        return session;
    }
}
