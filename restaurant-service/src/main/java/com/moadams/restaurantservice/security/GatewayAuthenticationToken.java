package com.moadams.restaurantservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom Authentication token to hold user details forwarded by the API Gateway.
 * This token will represent an already authenticated user.
 */
public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;
    private final Object credentials;

    public GatewayAuthenticationToken(String email, String roles) {
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
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getEmail() {
        return (String) getPrincipal();
    }
}