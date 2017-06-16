package com.staniul.security.web.controller;

public class TokenContainer {
    private String token;

    public TokenContainer(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
