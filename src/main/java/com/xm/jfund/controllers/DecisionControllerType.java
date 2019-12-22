package com.xm.jfund.controllers;

public enum DecisionControllerType {

    DEFAULT("default", DefaultController.class);

    private final String mType;
    private final Class<? extends DecisionController> mControllerClass;

    <T extends DecisionController> DecisionControllerType(final String type, final Class<T> clazz) {
        mType = type;
        mControllerClass = clazz;
    }

    public String getType() {
        return mType;
    }

    public Class<? extends DecisionController> getControllerClass() {
        return mControllerClass;
    }

    public static DecisionControllerType forType(final String type) {
        for (final DecisionControllerType controllerType : DecisionControllerType.values()) {
            if (controllerType.getType().equalsIgnoreCase(type)) {
                return controllerType;
            }
        }
        throw new AssertionError(String.format("Wrong controller type: %s", type));
    }

}
