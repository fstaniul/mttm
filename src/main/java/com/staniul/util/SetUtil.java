package com.staniul.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for {@code Set} actions.
 */
public class SetUtil {
    /**
     * Produces {@code HashSet} that is intersection of 2 sets. The order does not matter.
     *
     * @param a   First set.
     * @param b   Seconds set.
     * @param <T> Type of objects contained in sets. Does not matter here, just for creation of a new set.
     *
     * @return {@code HashSet} that is intersection of 2 given sets.
     */
    public static <T> HashSet<T> intersection(Set<T> a, Set<T> b) {
        boolean isALarger = a.size() > b.size();

        HashSet<T> result = new HashSet<>(isALarger ? b.size() : a.size());

        if (isALarger) b.stream().filter(a::contains).forEach(result::add);
        else a.stream().filter(b::contains).forEach(result::add);

        return result;
    }

    /**
     * Counts the size of intersection of 2 sets. Does not create a new set. Order of sets does not matter.
     *
     * @param a First set.
     * @param b Second set.
     *
     * @return {@code Long} that is the size of intersection of 2 sets. New set is not created here so does not use more
     * space.
     */
    public static long countIntersection(Set<?> a, Set<?> b) {
        boolean isALarger = a.size() > b.size();

        if (isALarger) return b.stream().filter(a::contains).count();
        else return a.stream().filter(b::contains).count();
    }

    /**
     * Returns a set of items contained in array of a specified type.
     *
     * @param array Array of a specified type.
     * @param <T>   type of elements in array.
     *
     * @return Set created from elements in array.
     */
    @SafeVarargs
    public static <T> Set<T> arrayAsSet(T... array) {
        return Arrays.stream(array).collect(Collectors.toSet());
    }
}
