package com.staniul.teamspeak.security;

/**
 * Access check that allways gives access.
 * @param <T> Type of items that are being checked.
 */
public class PermitAllAccessCheck <T> implements AccessCheck<T> {
    @Override
    public Boolean apply(T client) {
        return true;
    }
}
