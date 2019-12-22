package com.xm.jfund.controllers;

public class OrderVolumeCalculationResult {

    private final OrderVolumeCalculationStatus mOrderVolumeCalculationStatus;
    private final double mOrderVolume;

    public static OrderVolumeCalculationResult create(final OrderVolumeCalculationStatus orderVolumeCalculationStatus, final double orderVolume) {
        return new OrderVolumeCalculationResult(orderVolumeCalculationStatus, orderVolume);
    }

    private OrderVolumeCalculationResult(final OrderVolumeCalculationStatus orderVolumeCalculationStatus, final double orderVolume) {
        mOrderVolumeCalculationStatus = orderVolumeCalculationStatus;
        mOrderVolume = orderVolume;
    }

    public OrderVolumeCalculationStatus getOrderVolumeCalculationStatus() {
        return mOrderVolumeCalculationStatus;
    }

    public double getOrderVolume() {
        return mOrderVolume;
    }

    public boolean isValid() {
        return mOrderVolumeCalculationStatus.isValid();
    }
}
