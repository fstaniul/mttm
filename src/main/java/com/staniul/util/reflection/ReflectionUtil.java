package com.staniul.util.reflection;

import org.apache.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to use with Reflection
 */
public class ReflectionUtil {
    private static Logger log = Logger.getLogger(ReflectionUtil.class);

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

    /**
     * Gets set of fields that are annotated with {@code annotationClass}
     * @param aClass A class of which we are looking for fields.
     * @param annotationClass Annotation class to be present on a field.
     * @return Set of fields with desired annotation present.
     */
    public static Set<Field> getFieldsAnnotatedWith (Class<?> aClass, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(aClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(annotationClass)).collect(Collectors.toSet());
    }

    /**
     * Gets declared fields of a class and returns them as set.
     * @param aClass A class for which we get fields.
     * @return Set of fields of a class.
     */
    public static Set<Field> getFields (Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredFields()).collect(Collectors.toSet());
    }

    /**
     * Creates a new instance of class and returns null when exception. Handles exception by printing them to log4j.
     * @param aClass A class for which we will create new instance.
     * @param <T> A type of new instance object.
     * @return New object of desired type.
     */
    public static <T> T createDefaultOrNull (Class<T> aClass) {
        try {
            return aClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Failed to instantiate object of a class: " + aClass.getName(), e);
            return null;
        }
    }

    /**
     * Sets value of a field in a target object.
     * @param field Field to set.
     * @param target Target object.
     * @param value New value of a field.
     */
    public static void setField (Field field, Object target, Object value) {
        boolean acc = field.isAccessible();
        if (!acc) field.setAccessible(true);

        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            log.error("Failed to set value of a field: " + field, e);
        }

        field.setAccessible(acc);
    }
}
