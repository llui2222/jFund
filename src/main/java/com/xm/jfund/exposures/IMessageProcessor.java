package com.xm.jfund.exposures;

public interface IMessageProcessor extends Runnable {

    boolean connect();

    void disconnect();

    void timeToDie();

    void cleanup();

    String getServerName();
}
