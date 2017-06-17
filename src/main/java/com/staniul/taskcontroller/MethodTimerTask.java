package com.staniul.taskcontroller;

import com.staniul.util.reflection.MethodContainer;

import java.lang.reflect.Method;
import java.util.TimerTask;

public class MethodTimerTask extends TimerTask {
    private MethodContainer methodContainer;

    public MethodTimerTask(Method method, Object target) {
        this.methodContainer = new MethodContainer(target, method);
    }

    @Override
    public void run() {
        methodContainer.invoke();
    }

    @Override
    public String toString() {
        return methodContainer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MethodTimerTask &&
                ((MethodTimerTask) obj).methodContainer.equals(methodContainer);
    }
}
