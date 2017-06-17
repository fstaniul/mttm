package com.staniul.util.reflection;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodContainer {
    private static Logger log = Logger.getLogger(MethodContainer.class);

    private Object target;
    private Method method;

    public MethodContainer(Object target, Method method) {
        this.target = target;
        this.method = method;
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

    public void invoke (Object... parameters) {
        try {
            method.invoke(target, parameters);
        } catch (IllegalAccessException e) {
            log.error("Method is private and cannot be invoked from outside class: " + this, e);
        } catch (InvocationTargetException e) {
            log.error("Error during method call: " + this, e);
        }
    }
}

