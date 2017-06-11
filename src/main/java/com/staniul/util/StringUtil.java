package com.staniul.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for String class.
 */
public class StringUtil {
    /**
     * Splits the string on {@code split} characters. New strings length is not greater then {@code size}. If in last
     * {@code size} characters {@code split} characters were not found then the string will be cut to be length of
     * {@code size}.
     *
     * @param string String to split.
     * @param split  Separator.
     * @param size   Maximum size of substrings.
     *
     * @return Array of strings that are substrings of {@code string} of max length {@code size}. Separator chars are
     * omitted if splited on separator characters.
     */
    public static String[] splitOnSize(String string, String split, int size) {
        List<String> result = new LinkedList<>();
        int currentStart = 0;

        while (currentStart + size < string.length()) {
            int cutIndex = string.lastIndexOf(split, currentStart + size);
            if (cutIndex < currentStart) {
                cutIndex = currentStart + size;
                result.add(string.substring(currentStart, cutIndex));
                currentStart = cutIndex;
            }
            else {
                result.add(string.substring(currentStart, cutIndex));
                currentStart = cutIndex + split.length();
            }
        }

        if (currentStart < string.length()) {
            result.add(string.substring(currentStart));
        }

        String[] strings = new String[result.size()];
        return result.toArray(strings);
    }
}
