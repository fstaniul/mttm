package com.staniul.security.web.config;

import com.staniul.security.jwt.JWTTokenProvider;
import com.staniul.security.web.Teamspeak3AuthenticationFilter;
import com.staniul.security.web.provider.Teamspeak3AuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebMvc
@EnableWebSecurity
public class MTTMSpringWebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final JWTTokenProvider jwtTokenProvider;
    private final Teamspeak3AuthenticationProvider authProvider;
    private final Teamspeak3AuthenticationFilter authFilter;

    @Autowired
    public MTTMSpringWebSecurityConfiguration(JWTTokenProvider jwtTokenProvider,
                                              Teamspeak3AuthenticationProvider authProvider,
                                              Teamspeak3AuthenticationFilter authFilter) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authProvider = authProvider;
        this.authFilter = authFilter;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/api/auth");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        assert jwtTokenProvider != null;
        assert authFilter != null;

        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .anonymous().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedEntryPoint());

        http.addFilterBefore(authFilter, BasicAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        assert authProvider != null;
        auth.authenticationProvider(authProvider);
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request,response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
