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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userEmail = request.getHeader("X-Auth-User-Email");
        String userRoles = request.getHeader("X-Auth-User-Roles");

        if (userEmail != null && !userEmail.isEmpty() && userRoles != null && !userRoles.isEmpty()) {
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(userEmail, userRoles);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            logger.debug("X-Auth-User-Email or X-Auth-User-Roles headers missing. Proceeding without authentication context.");
        }

        filterChain.doFilter(request, response);
    }
}