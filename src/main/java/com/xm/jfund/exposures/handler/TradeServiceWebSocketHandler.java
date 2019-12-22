package com.xm.jfund.exposures.handler;

import com.xm.jfund.client.trade.model.Trade;
import com.xm.jfund.queuemessages.TradeServiceConnectionEstablished;
import com.xm.jfund.queuemessages.TradeServiceError;
import com.xm.jfund.queuemessages.TradeServiceMessage;
import com.xm.jfund.queuemessages.TradeServicePosition;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;

public class TradeServiceWebSocketHandler implements StompSessionHandler {

    private final BlockingQueue<TradeServiceMessage> processorQueue;

    private TradeServiceWebSocketHandler(final BlockingQueue<TradeServiceMessage> processorQueue) {
        this.processorQueue = processorQueue;
    }

    public static TradeServiceWebSocketHandler create(final BlockingQueue<TradeServiceMessage> processorQueue) {
        return new TradeServiceWebSocketHandler(processorQueue);
    }

    @Override
    public void afterConnected(final StompSession session, final StompHeaders connectedHeaders) {
        processorQueue.add(TradeServiceConnectionEstablished.create(session));
    }

    @Override
    public void handleException(final StompSession session, final StompCommand command, final StompHeaders headers, final byte[] payload, final Throwable exception) {
        processorQueue.add(TradeServiceError.create(exception));
    }

    @Override
    public void handleTransportError(final StompSession session, final Throwable exception) {
        processorQueue.add(TradeServiceError.create(exception));
    }

    @Override
    public Type getPayloadType(final StompHeaders headers) {
        return Trade.class;
    }

    @Override
    public void handleFrame(final StompHeaders headers, final Object payload) {
        processorQueue.add(TradeServicePosition.create((Trade) payload));
    }
}