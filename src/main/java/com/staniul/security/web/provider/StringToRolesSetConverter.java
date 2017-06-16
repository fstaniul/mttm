package com.staniul.security.web.provider;

import com.staniul.xmlconfig.convert.StringToTypeConverter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class StringToRolesSetConverter implements StringToTypeConverter<Set<String>> {
    @Override
    public Set<String> convert(String entry) {
        return Arrays.stream(entry.split(",")).map(s -> "ROLE_" + s).collect(Collectors.toSet());
    }
}
