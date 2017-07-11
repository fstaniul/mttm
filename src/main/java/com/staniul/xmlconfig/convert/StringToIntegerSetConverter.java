package com.staniul.xmlconfig.convert;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class StringToIntegerSetConverter implements StringToTypeConverter<Set<Integer>> {
    @Override
    public Set<Integer> convert(String entry) {
        return Arrays.stream(entry.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
    }
}
