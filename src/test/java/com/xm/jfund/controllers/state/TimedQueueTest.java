package com.xm.jfund.controllers.state;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TimedQueueTest {

    @Test
    public void testGetItemAndIncrementDelay_firstItemIsNotDelayed() {
        final double value = 1.4;
        final long oneHourInMillis = 60_000 * 60;
        final TimedQueue<Double> timedQueue = new TimedQueue<>(oneHourInMillis);
        timedQueue.add(value);
        final Double poll = timedQueue.getItemAndIncrementDelay();
        assertTrue(poll == value);
    }

    @Test
    public void testGetItemAndIncrementDelay_itemsNeverDelayed() {
        final double value = 1.4;
        final TimedQueue<Double> timedQueue = new TimedQueue<>(0);
        timedQueue.add(value);
        timedQueue.add(value * 2);
        timedQueue.add(value * 3);
        assertTrue(Double.compare(timedQueue.getItemAndIncrementDelay(), value) == 0);
        assertTrue(Double.compare(timedQueue.getItemAndIncrementDelay(), value * 2) == 0);
        assertTrue(Double.compare(timedQueue.getItemAndIncrementDelay(), value * 3) == 0);
    }

    @Test
    public void testGetItemAndIncrementDelay_pollCallsIncrementMethod() {
        final TimedQueue<Double> timedQueue = spy(new TimedQueue<>(1));

        timedQueue.poll();
        verify(timedQueue, times(1)).getItemAndIncrementDelay();
    }

    @Test
    public void testGetItemAndIncrementDelay_removeCallsIncrementMethod() {
        final TimedQueue<Double> timedQueue = spy(new TimedQueue<>(1));

        timedQueue.remove();
        verify(timedQueue, times(1)).getItemAndIncrementDelay();
    }

    @Test
    public void testGetItemAndIncrementDelay_secondItemIsDelayedBy2Seconds() throws InterruptedException {
        final double value = 1.4;
        final double secondValue = 2.3;
        final long twoSecondsInMillis = 2_000;
        final TimedQueue<Double> timedQueue = new TimedQueue<>(twoSecondsInMillis);
        timedQueue.add(value);
        timedQueue.add(secondValue);
        final Double poll = timedQueue.getItemAndIncrementDelay();
        assertTrue(poll == value);

        assertNull(timedQueue.getItemAndIncrementDelay());

        final Double secondPoll = waitAndGetSecondPoll(timedQueue);

        assertTrue(secondPoll == secondValue);
    }

    private Double waitAndGetSecondPoll(final TimedQueue<Double> timedQueue) throws InterruptedException {
        Double secondPoll;
        int count = 0;
        final int max = 4;
        final long sleepAmount = 500;
        while ((secondPoll = timedQueue.getItemAndIncrementDelay()) == null && count < max) {
            count++;
            Thread.sleep(sleepAmount);
        }
        return secondPoll;
    }
}
