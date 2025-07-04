package com.moadams.orderservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;
    private final String credentials;

    private String userEmail;
    private String userId;

    public JwtAuthenticationToken(
            String principal,
            String credentials,
            Collection<? extends GrantedAuthority> authorities,
            String userEmail,
            String userId) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.userEmail = userEmail;
        this.userId = userId;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserId() {
        return userId;
    }
}