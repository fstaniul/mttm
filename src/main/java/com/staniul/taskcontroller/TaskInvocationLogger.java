package com.staniul.taskcontroller;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TaskInvocationLogger {
    private static Logger log = Logger.getLogger(TaskInvocationLogger.class);

    @Pointcut("execution(public void * ()) && @annotation(com.staniul.taskcontroller.Task)")
    public void taskCall () {}

    @Before(value = "taskCall()", argNames = "joinPoint")
    public void beforeTaskCallLogger (JoinPoint joinPoint) {
        log.info(String.format("Calling task %s", joinPoint.getSignature().toShortString()));
    }
}
