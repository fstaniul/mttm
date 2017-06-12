package com.staniul.util;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodContainer {
    private static Logger log = Logger.getLogger(MethodContainer.class);

    private Object target;
    private Method method;
    private int exceptionCounter;

    public MethodContainer(Object target, Method method) {
        this.target = target;
        this.method = method;
        exceptionCounter = 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MethodContainer &&
                ((MethodContainer) obj).method.equals(method) &&
                ((MethodContainer) obj).target.equals(target);
    }

    @Override
    public String toString() {
        return String.format("Method: %s, Target: %s", method, target);
    }

    public boolean isCausingExceptionRegularly() {
        return exceptionCounter > 1;
    }

    public void invoke (Object... parameters) {
        try {
            method.invoke(target, parameters);
            exceptionCounter = 0;
        } catch (IllegalAccessException e) {
            exceptionCounter ++;
            log.error("Failed to invoke event: " + this, e);
        } catch (InvocationTargetException e) {
            exceptionCounter ++;
            log.error("Error during event call: " + this, e);
        }
    }
}

