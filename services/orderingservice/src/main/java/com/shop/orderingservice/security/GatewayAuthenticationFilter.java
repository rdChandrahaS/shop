package com.shop.orderingservice.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${gateway.internal.secret:}")
    private String gatewayInternalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String gatewaySecret = request.getHeader("X-Gateway-Secret");
        if (gatewayInternalSecret.isBlank() || !gatewayInternalSecret.equals(gatewaySecret)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Requests must pass through the API gateway");
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String roleStr = request.getHeader("X-User-Role");

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream((roleStr == null ? "" : roleStr).split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
