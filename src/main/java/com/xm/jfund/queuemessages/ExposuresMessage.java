package com.xm.jfund.queuemessages;

public interface ExposuresMessage {

    void accept(final ExposuresVisitor visitor);
}
