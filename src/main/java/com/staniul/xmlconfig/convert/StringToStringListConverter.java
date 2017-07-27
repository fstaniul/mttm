package com.staniul.xmlconfig.convert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringToStringListConverter implements StringToTypeConverter<List<String>> {
    @Override
    public List<String> convert(String entry) {
        return Arrays.stream(entry.split(",")).collect(Collectors.toList());
    }
}
