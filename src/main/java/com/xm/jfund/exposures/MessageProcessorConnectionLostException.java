package com.xm.jfund.exposures;

/**
 * Exception representing if a connection was lost
 */
public class MessageProcessorConnectionLostException extends RuntimeException {
    MessageProcessorConnectionLostException(final String message) {
        super(message);
    }
}
