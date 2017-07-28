package com.staniul.api.security;

import com.staniul.api.security.auth.ApiClientDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthUtil {
    public static ApiClientDetails getClientDetails (Authentication authentication) {
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        return (ApiClientDetails) token.getPrincipal();
    }
}
