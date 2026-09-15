package com.shop.foodservice.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${gateway.internal.secret}")
    private String gatewayInternalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String gatewaySecret = request.getHeader("X-Gateway-Secret");
        if (gatewayInternalSecret == null || !gatewayInternalSecret.equals(gatewaySecret)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Requests must pass through the API gateway");
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String roleStr = request.getHeader("X-User-Role");

        if (userId != null && !userId.isEmpty()) {
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            if (roleStr != null && !roleStr.isEmpty()) {
                authorities = Arrays.stream(roleStr.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }
}
