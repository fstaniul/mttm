package com.staniul.security.web;

import com.staniul.security.jwt.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class Teamspeak3AuthenticationFilter extends GenericFilterBean {
    private final JWTTokenProvider jwtTokenProvider;

    @Autowired
    public Teamspeak3AuthenticationFilter(JWTTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        String token = httpRequest.getHeader("X-Auth-Token");

        if (token != null) {
            Teamspeak3AuthenticationToken authToken = jwtTokenProvider.parseToken(token);
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
