package com.xm.jfund.utils;

import jxmUtils.RabbitMQUtils;
import jxmUtils.RabbitMQUtils.RabbitMQConnectionState;

public final class JFundRabbitMQUtils {

    private static final String sAMQP_PRODUCER_URI = String.format("amqp://%s:%s@%s/%s", "jAnalystServer", "PmxvnI", "rabbitmq:5672", "jAnalystVhost");

    public static RabbitMQConnectionState createProducerConnection(final String queueName) {
        return RabbitMQUtils.createProducerConnection(sAMQP_PRODUCER_URI, queueName);
    }

    public static String getQueueName(final String profile, final int strategyId) {
        return String.format("%s_jFundStrategy_%d", profile, strategyId);
    }

    public static String getRoutingKey(final String profile, final int strategyId) {

        return String.format("%s_jFundStrategy_%d", profile, strategyId);
    }
}