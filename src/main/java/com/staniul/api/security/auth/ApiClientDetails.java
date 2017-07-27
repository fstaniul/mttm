package com.staniul.api.security.auth;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiClientDetails {
    private int databaseId;
    private List<String> authorities;

    public ApiClientDetails(int databaseId, List<String> authorities) {
        this.databaseId = databaseId;
        this.authorities = authorities;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public List<String> getAuthorities () {
        return authorities;
    }

    public List<GrantedAuthority> getGrantedAuthorities() {
        return authorities.stream()
                .map(authorityMapper())
                .collect(Collectors.toList());
    }

    private Function<String, GrantedAuthority> authorityMapper () {
        return i -> (GrantedAuthority) () -> i;
    }
}
