package com.staniul.xmlconfig.convert;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Converter from String to a type.
 * This class is thread safe, but another synchronization would be needed when adding new converters.
 * You can store multiple converters in this one.
 */
public class StringToTypeConverterFactory {
    private static final Map<Class<?>, StringToTypeConverter<?>> defaultTypeConverter = new HashMap<>();

    static {
        StringToTypeConverter<Integer> intConverter = Integer::parseInt;
        defaultTypeConverter.put(Integer.class, intConverter);
        defaultTypeConverter.put(Integer.TYPE, intConverter);

        StringToTypeConverter<Double> doubleConverter = Double::parseDouble;
        defaultTypeConverter.put(Double.class, doubleConverter);
        defaultTypeConverter.put(Double.TYPE, doubleConverter);

        StringToTypeConverter<Float> floatConverter = Float::parseFloat;
        defaultTypeConverter.put(Float.class, floatConverter);
        defaultTypeConverter.put(Float.TYPE, floatConverter);

        StringToTypeConverter<Long> longConverter = Long::parseLong;
        defaultTypeConverter.put(Long.class, longConverter);
        defaultTypeConverter.put(Long.TYPE, longConverter);

        StringToTypeConverter<Byte> byteConverter = Byte::parseByte;
        defaultTypeConverter.put(Byte.class, byteConverter);
        defaultTypeConverter.put(Byte.TYPE, byteConverter);

        StringToTypeConverter<Boolean> booleanConverter = Boolean::parseBoolean;
        defaultTypeConverter.put(Boolean.TYPE, booleanConverter);
        defaultTypeConverter.put(Boolean.class, booleanConverter);

        StringToTypeConverter<Short> shortConverter = Short::parseShort;
        defaultTypeConverter.put(Short.class, shortConverter);
        defaultTypeConverter.put(Short.TYPE, shortConverter);

        defaultTypeConverter.put(String.class, StringToTypeConverter.identity());
    }

    private Map<Class<?>, StringToTypeConverter<?>> converterMap;

    public StringToTypeConverterFactory() {
        converterMap = new HashMap<>();
        converterMap.putAll(defaultTypeConverter);
    }

    public StringToTypeConverterFactory add(Class<?> tClass, StringToTypeConverter<?> converter) {
        converterMap.put(tClass, converter);
        return this;
    }

    public <T> T convert(Class<T> tClass, String strValue) {
        StringToTypeConverter<?> converter = converterMap.get(tClass);
        if (converter == null) return null;
        try {
            return (T) converter.convert(strValue);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
