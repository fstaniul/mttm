package com.staniul.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converter from String to a type.
 * This class is thread safe, but another synchronization would be needed when adding new converters.
 * You can store multiple converters in this one.
 */
public class StringToTypeConverter {
    private Map<Class<?>, Function<String, ?>> converterMap;

    public StringToTypeConverter() {
        converterMap = new HashMap<>();
    }

    public <T> StringToTypeConverter add(Class<T> tClass, Function<String, T> tFunction) {
        if (tClass.isPrimitive() || tClass.equals(String.class))
            throw new IllegalArgumentException("Strings and Primitives are not supported.");

        converterMap.putIfAbsent(tClass, tFunction);

        return this;
    }

    public <T> T convert(Class<T> tClass, String strValue) {
        return (T) converterMap.get(tClass).apply(strValue);
    }
}
