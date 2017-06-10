package com.staniul.teamspeak.security;

public class PermitAllAccessCheck <T> implements AccessCheck<T> {
    @Override
    public Boolean apply(T client) {
        return true;
    }
}
