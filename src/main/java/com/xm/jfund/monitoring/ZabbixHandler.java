package com.xm.jfund.monitoring;

import com.xm.jfund.zabbixobjects.ZabbixMessage;
import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class ZabbixHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ZabbixHandler.class);

    public static ZabbixHandler create(
        final BlockingQueue<ZabbixMessage> messageQueue,
        final ZabbixMessage discoveryMessage,
        final String zabbixHostName,
        final int zabbixPort,
        final String zabbixProjectHost) {

        final ZabbixSender zabbixSender = new ZabbixSender(zabbixHostName, zabbixPort);

        final Queue<ZabbixMessage> internalMessageCache = new LinkedList<>();
        internalMessageCache.add(discoveryMessage);

        final boolean timeToDie = false;

        return new ZabbixHandler(messageQueue, zabbixSender, zabbixProjectHost, internalMessageCache, timeToDie);
    }

    public void timeToDie() {
        mTimeToDie = true;
    }

    @Override
    public void run() {

        logger.info("Zabbix handler has started...");
        try {
            while (!mTimeToDie) {

                ZabbixMessage msg = null;

                try {
                    msg = mMessageQueue.poll(sPollingTimeForIncomingMessages, TimeUnit.MILLISECONDS);
                }
                catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                if (msg != null) {
                    mInternalMessageCache.add(msg);
                }

                while (!mInternalMessageCache.isEmpty()) {

                    final ZabbixMessage currentMessage = mInternalMessageCache.peek();

                    final boolean transmissionSucceeded;
                    try {
                        final DataObject dataObject = prepareZabbixDataObject(mZabbixProjectHost, currentMessage.getKey(), currentMessage.getValue());
                        transmissionSucceeded = mZabbixSender.send(dataObject).success();
                        if (transmissionSucceeded) {
                            mInternalMessageCache.remove();
                        }
                        else {
                            break;
                        }
                    }
                    catch (final Throwable e) {
                        //Zabbix 3rd party library is very buggy
                        //sometimes throws index out of bounds exception
                        //for now we will just retry.
                        logger.warn(String.format("Issues sending to Zabbix: %s", e.getMessage()));
                    }
                }
            }
        }

        finally {
            logger.info("Zabbix handler has ended.");
        }
    }

    private static final int sPollingTimeForIncomingMessages = 1000;

    private DataObject prepareZabbixDataObject(final String projectHost, final String keyToSend, final String valueToSend) {

        return DataObject.builder()
            .host(projectHost)
            .key(keyToSend)
            .value(valueToSend)
            .build();
    }

    private ZabbixHandler(
        final BlockingQueue<ZabbixMessage> messageQueue,
        final ZabbixSender zabbixSender,
        final String zabbixProjectHost,
        final Queue<ZabbixMessage> internalMessageCache,
        final boolean timeToDie) {

        mMessageQueue = messageQueue;
        mZabbixSender = zabbixSender;
        mZabbixProjectHost = zabbixProjectHost;
        mInternalMessageCache = internalMessageCache;
        mTimeToDie = timeToDie;
    }

    private final BlockingQueue<ZabbixMessage> mMessageQueue;
    private final ZabbixSender mZabbixSender;
    private final String mZabbixProjectHost;
    private final Queue<ZabbixMessage> mInternalMessageCache;
    private volatile boolean mTimeToDie;
}
