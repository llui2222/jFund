package com.xm.jfund.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Utilities for threads
 */
public class ThreadUtils {
    private static final Logger logger = LoggerFactory.getLogger(ThreadUtils.class);

    /**
     * Log all thread stacks currently running for debugging.
     */
    public static void logThreadStacks() {
        logger.info("++++++++++++++ Thread Dump Start ++++++++++++++");
        final Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (final Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            logger.info("***********************************************");
            logger.info("Thread name: " + entry.getKey().getName());
            for (final StackTraceElement element : entry.getValue()) {
                logger.info(element.toString());
            }
        }
        logger.info("++++++++++++++ Thread Dump End ++++++++++++++");
    }
}
