package com.staniul.util;

import java.util.HashSet;
import java.util.Set;

public class SetsUtil {
    public static <T> HashSet<T> intersection (Set<T> a, Set<T> b) {
        boolean isALarger = a.size() > b.size();

        HashSet<T> result = new HashSet<>(isALarger ? b.size() : a.size());

        if (isALarger) b.stream().filter(a::contains).forEach(result::add);
        else a.stream().filter(b::contains).forEach(result::add);

        return result;
    }

    public static long countIntersection (Set<?> a, Set<?> b) {
        boolean isALarger = a.size() > b.size();

        if (isALarger) return b.stream().filter(a::contains).count();
        else return a.stream().filter(b::contains).count();
    }
}
