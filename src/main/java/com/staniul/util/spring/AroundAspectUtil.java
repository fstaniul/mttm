package com.staniul.util.spring;

import com.staniul.teamspeak.commands.Teamspeak3Command;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for Spring AOP.
 */
public class AroundAspectUtil {
    /**
     * Gets annotation from method that is a pointcut in around aspect.
     *
     * @param pjp             Proceeding Join Point of @Around aspect.
     * @param annotationClass Class of annotation that we are looking for.
     * @param <T>             Type of annotation.
     * @return Annotation or null if annotation was not found.
     */
    public static <T extends Annotation> T getAnnotationOfAspectMethod(ProceedingJoinPoint pjp, Class<T> annotationClass) {
        Method method = getTargetMethodOfAspect(pjp);
        return method.getAnnotation(annotationClass);
    }

    /**
     * Gets annotations of given type from method that is a pointcut in around aspect.
     *
     * @param pjp             Proceeding Join Point of @Around aspect.
     * @param annotationClass Class of annotation that we are looking for.
     * @param <T>             Type of annotation
     * @return List of annotations that are present. Can be empty if there are no annotations.
     */
    public static <T extends Annotation> List<T> getAnnotationsOfAspectMethod(ProceedingJoinPoint pjp, Class<T> annotationClass) {
        Method method = getTargetMethodOfAspect(pjp);
        T[] annotations = method.getAnnotationsByType(annotationClass);
        return Arrays.asList(annotations);
    }

    /**
     * Gets target method of @Around aspect from ProceedingJoinPoint.
     *
     * @param pjp Proceeding Join Point of @Around aspect.
     * @return Method that is the proceeding method in this @Around aspect.
     */
    public static Method getTargetMethodOfAspect(ProceedingJoinPoint pjp) {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        return methodSignature.getMethod();
    }
}
