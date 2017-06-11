package com.staniul.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to use with Reflection
 */
public class ReflectionUtil {
    /**
     * Returns set of methods in a class {@code aClass} that are annotated with {@code annotationClass}
     * @param aClass A class in which we look for methods.
     * @param annotationClass Annotation of searched methods.
     * @return Set of methods annotated with a {@code annotationClass} in class {@code aClass}
     */
    public static Set<Method> getMethodsAnnotatedWith(Class<?> aClass, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(aClass.getMethods())
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .collect(Collectors.toSet());
    }

    /**
     * Assigns parameters to a method if they might be in different order.
     * Each object needs to be of a different class!
     *
     * @param params Params to be assigned.
     *
     * @return Array of objects that are parameters assigned from list.
     */
    public static Object[] assignParameters(List<Object> params, Method method) {
        Map<Class<?>, Object> classObjectMap = new HashMap<>(params.size() * 7 / 10 + 1);
        params.forEach(p -> classObjectMap.putIfAbsent(p.getClass(), p));

        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] result = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            result[i] = classObjectMap.get(paramTypes[i]);
        }

        return result;
    }
}
