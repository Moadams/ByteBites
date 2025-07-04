package com.moadams.orderservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;
    private final String credentials;

    private String userEmail;
    private String userId;

    public JwtAuthenticationToken(
            String email, String roles) {
        super(parseRoles(roles));
        this.principal = email;
        this.credentials = null;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> parseRoles(String rolesString) {
        if (rolesString == null || rolesString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleNames = Stream.of(rolesString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        return roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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