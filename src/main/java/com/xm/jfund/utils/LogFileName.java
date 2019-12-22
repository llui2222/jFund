package com.xm.jfund.utils;

public enum LogFileName {

    DECISION("Strategy_%d_decision"),
    ACTIVITY("Strategy_%d_activity_exposure"),
    ACCUMULATION("Strategy_%d_accumulation_exposure");

    private final String mFileName;

    LogFileName(final String fileName) {
        mFileName = fileName;
    }

    public String forStrategy(final int strategyId) {
        return String.format(mFileName, strategyId);
    }

}
