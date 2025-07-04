package com.moadams.restaurantservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security filter to extract user information from custom headers
 * forwarded by the API Gateway and set it in the SecurityContext.
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userEmail = request.getHeader("X-Auth-User-Email");
        String userRoles = request.getHeader("X-Auth-User-Roles");

        if (userEmail != null && !userEmail.isEmpty() && userRoles != null && !userRoles.isEmpty()) {
            GatewayAuthenticationToken authentication = new GatewayAuthenticationToken(userEmail, userRoles);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            logger.debug("X-Auth-User-Email or X-Auth-User-Roles headers missing. Proceeding without authentication context.");
        }

        filterChain.doFilter(request, response);
    }
}