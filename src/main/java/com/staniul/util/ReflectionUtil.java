package com.staniul.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionUtil {
    public static Set<Method> getMethodsAnnotatedWith (Class<?> aClass, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(aClass.getMethods())
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .collect(Collectors.toSet());
    }
}
