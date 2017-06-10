package com.staniul.core.security;

import java.util.function.Function;

/**
 * Checks access of a type to a resource.
 *
 * @param <T> type of object to check its access to resource.
 */
public interface AccessCheck<T> extends Function<T, Boolean> {

}
