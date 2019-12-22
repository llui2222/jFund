package com.xm.jfund.utils;

public enum LogExposuresMode {
    FULL(LogExposuresMode.sFull),
    ACCUMULATION(LogExposuresMode.sAccumulation),
    NONE(LogExposuresMode.sNone);

    @Override
    public String toString() {
        return mType;
    }

    public static LogExposuresMode fromString(final String type) {

        switch (type) {

            case sFull: return FULL;
            case sAccumulation: return ACCUMULATION;
            case sNone: return NONE;
            default: throw new IllegalArgumentException(String.format("Unknown conversion from String '%s' to LogExposuresMode. Please use one of the valid options in -logExposures: %s|%s|%s", type, sFull, sAccumulation, sNone));
        }
    }

    private static final String sFull = "full";
    private static final String sAccumulation = "accumulation";
    private static final String sNone = "none";

    LogExposuresMode(final String type) {
        mType = type;
    }

    private final String mType;
}