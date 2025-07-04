package com.moadams.orderservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String X_AUTH_USER_ID = "X-Auth-User-Id";
    private static final String X_AUTH_USER_EMAIL = "X-Auth-User-Email";
    private static final String X_AUTH_USER_ROLES = "X-Auth-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(X_AUTH_USER_ID);
        String userEmail = request.getHeader(X_AUTH_USER_EMAIL);
        String userRoles = request.getHeader(X_AUTH_USER_ROLES);

        if (userId != null && !userId.isEmpty() && userEmail != null && !userEmail.isEmpty()) {
            Collection<? extends GrantedAuthority> authorities = null;
            if (userRoles != null && !userRoles.isEmpty()) {
                authorities = Arrays.stream(userRoles.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }


            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    userId,
                    null,
                    authorities,
                    userEmail,
                    userId
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            System.out.println("Headers not present");
            // If headers are not present, it means either:
            // 1. The request did not go through the gateway's JWT validation filter (e.g., direct access, or unauthenticated route)
            // 2. The Gateway's filter failed or didn't forward headers.
            // For unauthenticated routes, SecurityContextHolder.getContext().setAuthentication(null) or not setting it is fine.
            // For authenticated routes without headers, access will eventually be denied by Spring Security's authorization.
        }

        filterChain.doFilter(request, response);
    }
}