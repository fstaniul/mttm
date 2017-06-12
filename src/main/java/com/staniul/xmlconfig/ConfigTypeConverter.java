package com.staniul.xmlconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ConfigTypeConverter {
    private Map<Class<?>, Function<String, ?>> converterMap;

    public ConfigTypeConverter () {
        converterMap = new HashMap<>();
    }

    public <T> ConfigTypeConverter add (Class<T> tClass, Function<String, T> tFunction) {
        if (tClass.isPrimitive() || tClass.equals(String.class))
            throw new IllegalArgumentException("Strings and Primitives are not supported.");

        converterMap.putIfAbsent(tClass, tFunction);

        return this;
    }

    public <T> T convert (Class<T> tClass, String strValue) {
        return (T) converterMap.get(tClass).apply(strValue);
    }
}
