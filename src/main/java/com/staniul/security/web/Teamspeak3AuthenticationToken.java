package com.staniul.security.web;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class Teamspeak3AuthenticationToken extends AbstractAuthenticationToken {
    private Integer databaseId;
    private String ip;

    public Teamspeak3AuthenticationToken(Integer databaseId, String ip, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.databaseId = databaseId;
        this.ip = ip;
    }

    @Override
    public Object getCredentials() {
        return databaseId;
    }

    @Override
    public Object getPrincipal() {
        return ip;
    }

    public Integer getDatabaseId() {
        return databaseId;
    }

    public String getIp() {
        return ip;
    }
}
