package com.xm.jfund.controllers.utilities;

public enum Danger {

    EXPOSURE_EXCEEDS_DANGER_LEVEL("Exposures exceed danger exposure level."),
    EXPOSURE_EXCEEDS_SINGLE_TRADE_DANGER("Exposures exceed single trade danger level.");

    private final String message;

    Danger(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
