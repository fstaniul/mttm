package com.staniul.security.web;

import com.staniul.teamspeak.query.Client;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class Teamspeak3AuthenticationToken extends AbstractAuthenticationToken {
    private final int databaseId;
    private final String ip;
    private final Client client;

    public Teamspeak3AuthenticationToken(int databaseId, String ip, Client client, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.databaseId = databaseId;
        this.ip = ip;
        this.client = client;
    }

    @Override
    public Object getCredentials() {
        return client;
    }

    @Override
    public Object getPrincipal() {
        return client.getDatabaseId();
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public String getIp() {
        return ip;
    }

    public Client getClient() {
        return client;
    }
}
