package com.xm.jfund.controllers;

public enum OrderVolumeCalculationStatus {
    OK(true),
    OK_EXECUTING_A_PREVIOUS_SPLIT_VOLUME(true),
    NOT_TRADING(false);


    private final boolean mValid;

    OrderVolumeCalculationStatus(final boolean valid) {
        mValid = valid;
    }

    public boolean isValid() {
        return mValid;
    }
}
