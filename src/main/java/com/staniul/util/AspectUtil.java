package com.staniul.util;

import com.staniul.teamspeak.commands.Teamspeak3Command;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AspectUtil {
    public static <T extends Annotation> T getAnnotationOfAspectMethod (ProceedingJoinPoint pjp, Class<T> annotationClass) {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        return method.getAnnotation(annotationClass);
    }
}
