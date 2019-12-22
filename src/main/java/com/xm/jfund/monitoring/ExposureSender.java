package com.xm.jfund.monitoring;

import com.xm.jfund.utils.EnvUtils;
import com.xm.jfund.utils.JFundRabbitMQUtils;
import jxmUtils.RabbitMQUtils;
import jxmUtils.RabbitMQUtils.RabbitMQConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** Created by msamatas on 11/07/17. */
public final class ExposureSender implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ExposureSender.class);

  public static ExposureSender create(
      final int strategyId, final BlockingQueue<byte[]> inBoundQueue) {

    final boolean timeToDie = false;

    return new ExposureSender(strategyId, timeToDie, inBoundQueue);
  }

  private static final int sPollingTimeForIncomingMessages = 1000;

  @Override
  public void run() {

    final String profile = EnvUtils.getActiveProfile();
    final String outboundQueueName = JFundRabbitMQUtils.getQueueName(profile, strategyId);
    final String routingKey = JFundRabbitMQUtils.getRoutingKey(profile, strategyId);

    logger.info("Exposure Sender has started for strategy {}...", strategyId);
    RabbitMQConnectionState rabbitMQConnectionState = null;
    try {
      logger.info("Connecting to queue {}...", outboundQueueName);

      while (!timeToDie) {

        byte[] entry = null;

        try {
          entry = inBoundQueue.poll(sPollingTimeForIncomingMessages, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
        }

        if (entry != null) {

          try {
            if (rabbitMQConnectionState == null || !rabbitMQConnectionState.mConnection.isOpen()) {
              rabbitMQConnectionState = JFundRabbitMQUtils.createProducerConnection(outboundQueueName);
            }

            rabbitMQConnectionState.mChannel.basicPublish(outboundQueueName, routingKey, null, entry);
          } catch (final Throwable ex) {
            logger.error(
                "Exception while sending exposures to RabbitMQ (queue {}). Continuing...",
                outboundQueueName,
                ex);
            cleanUpRabbitMQConnection(rabbitMQConnectionState);
            rabbitMQConnectionState = null;

            // Sleeping here to allow some time for the network or RabbitMQ to recover until next
            // connection retry.
            try {
              Thread.sleep(1000);
            } catch (final InterruptedException interruptedException) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }
    } finally {
      if (rabbitMQConnectionState != null) {
        cleanUpRabbitMQConnection(rabbitMQConnectionState);
      }
      logger.info("Exposure Sender has ended for strategy{}.", strategyId);
    }
  }

  public void timeToDie() {
    timeToDie = true;
  }

  private void cleanUpRabbitMQConnection(final RabbitMQConnectionState rabbitMQConnectionState) {

    try {
      RabbitMQUtils.closeMQResources(rabbitMQConnectionState.mConnection, rabbitMQConnectionState.mChannel);
    } catch (final Throwable ex) {
      logger.info(
          "Exception while trying to close RabbitMQ for strategy{}. Exception message was: "
              + ex.getMessage(),
          strategyId);
    }
  }

  private ExposureSender(
      final int strategyId, final boolean timeToDie, final BlockingQueue<byte[]> inBoundQueue) {

    this.timeToDie = timeToDie;
    this.inBoundQueue = inBoundQueue;
    this.strategyId = strategyId;
  }

  private volatile boolean timeToDie;
  private final BlockingQueue<byte[]> inBoundQueue;
  private final int strategyId;
}
