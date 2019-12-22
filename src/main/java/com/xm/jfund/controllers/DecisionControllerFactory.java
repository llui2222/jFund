package com.xm.jfund.controllers;

import java.text.MessageFormat;

public class DecisionControllerFactory {

    public static DecisionController create(final DecisionControllerType type) {
        try {
            return type.getControllerClass().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(MessageFormat.format("Error while instantiating decision controller class {0}. System will use the default Decision Controller", type.getControllerClass()), e);
        }
    }
}
