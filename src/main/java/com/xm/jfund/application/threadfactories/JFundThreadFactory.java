package com.xm.jfund.application.threadfactories;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * Thread factory for jfund so that we can name our threads appropriately
 */
public class JFundThreadFactory implements ThreadFactory {
    private final String threadName;
    private final List<Integer> threadNumbers;
    private int index;

    private JFundThreadFactory(final String threadName, final List<Integer> threadNumbers) {
        this.threadName = threadName;
        this.threadNumbers = threadNumbers;
        Objects.requireNonNull(threadName);
        Objects.requireNonNull(threadNumbers);
        this.index = 0;
    }

    public static JFundThreadFactory create(final String threadName, final List<Integer> threadNumbers) {
        return new JFundThreadFactory(threadName, threadNumbers);
    }

    /**
     * Create a new thread from the runnable.
     * The thread will be named "threadName_threadNumber".
     * If we ran out of numbers the name will be just "threadName"
     *
     * @param r runnable
     * @return thread
     */
    @Override
    public Thread newThread(final Runnable r) {

        final Thread t = new Thread(r);

        final String finalName;
        if (index >= 0 && index < threadNumbers.size()) {
            finalName = threadName + "_" + threadNumbers.get(index);

            index++;
        }
        else {
            finalName = threadName;
        }

        t.setName(finalName);

        return t;
    }
}
