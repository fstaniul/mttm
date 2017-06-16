package com.staniul.security.web.provider;

import org.springframework.security.core.GrantedAuthority;

public class Teamspeak3GrantedAuthority implements GrantedAuthority {
    private String authority;

    public Teamspeak3GrantedAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }
}
