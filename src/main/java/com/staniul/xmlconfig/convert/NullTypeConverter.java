package com.staniul.xmlconfig.convert;

public class NullTypeConverter implements StringToTypeConverter<Object> {
    @Override
    public Object convert(String entry) {
        return null;
    }
}
