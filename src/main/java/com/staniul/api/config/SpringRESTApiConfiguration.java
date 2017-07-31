package com.staniul.api.config;

import com.staniul.api.security.UnauthorizedAuthenticationEntryPoint;
import com.staniul.api.security.JwtAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SpringRESTApiConfiguration extends WebSecurityConfigurerAdapter {

    private UnauthorizedAuthenticationEntryPoint unauthorizedHandler;
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()

                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()

                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()

                .authorizeRequests()

                .antMatchers("/auth/**").permitAll()
                .antMatchers("/api/admin").hasRole("ROLE_MOD")
                .antMatchers("/api/owner").hasRole("ROLE_OWNER")

                .anyRequest().authenticated();


        http
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers().cacheControl();
    }

    @Autowired
    public void setUnauthorizedHandler(UnauthorizedAuthenticationEntryPoint unauthorizedHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Autowired
    public void setAuthenticationTokenFilter(JwtAuthenticationTokenFilter authenticationTokenFilter) {
        this.authenticationTokenFilter = authenticationTokenFilter;
    }
}
