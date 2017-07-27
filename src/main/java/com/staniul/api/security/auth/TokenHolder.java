package com.staniul.api.security.auth;

public class TokenHolder {
    private String token;

    public TokenHolder(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
