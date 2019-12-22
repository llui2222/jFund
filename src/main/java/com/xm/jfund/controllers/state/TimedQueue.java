package com.xm.jfund.controllers.state;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public final class TimedQueue<E> implements Queue<E> {
    private final long delayInMillis;
    private final Queue<E> internalQueue;
    private long currentDelay;

    TimedQueue(final long delayInMillis) {
        this.delayInMillis = delayInMillis;
        this.internalQueue = new LinkedList<>();
        currentDelay = 0;
    }

    @Override
    public int size() {
        return internalQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return internalQueue.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return internalQueue.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return internalQueue.iterator();
    }

    @Override
    public Object[] toArray() {
        return internalQueue.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        //noinspection RedundantCast,SuspiciousToArrayCall
        return internalQueue.toArray((T[]) a);
    }

    @Override
    public boolean add(final E e) {
        return internalQueue.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        return internalQueue.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return internalQueue.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return internalQueue.addAll(c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return internalQueue.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return internalQueue.retainAll(c);
    }

    @Override
    public void clear() {
        internalQueue.clear();
    }

    @Override
    public boolean offer(final E e) {
        return internalQueue.offer(e);
    }

    @Override
    public E remove() {
        return getItemAndIncrementDelay();
    }

    E getItemAndIncrementDelay() {
        final E item;
        final long now = System.currentTimeMillis();
        if (now >= currentDelay) {
            item = internalQueue.poll();
            currentDelay = now + delayInMillis;
        }
        else {
            item = null;
        }

        return item;
    }

    @Override
    public E poll() {
        return getItemAndIncrementDelay();
    }

    @Override
    public E element() {
        return internalQueue.element();
    }

    @Override
    public E peek() {
        return internalQueue.peek();
    }
}
