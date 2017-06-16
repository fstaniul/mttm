package com.staniul.xmlconfig.convert;

/**
 * Functional interface that converts string to a desired type.
 *
 * @param <T> Type of object to which conversion from string is made.
 */
public interface StringToTypeConverter<T> {
    static StringToTypeConverter<String> identity () {
        return e -> e;
    }

    T convert(String entry);
}