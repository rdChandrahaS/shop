package com.shop.apigatewayservice.filter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthHeaderFilter implements GlobalFilter, Ordered {

    @Value("${gateway.internal.secret}")
    private String internalSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = exchange.mutate().request(exchange.getRequest().mutate()
            .headers(headers -> {
                headers.remove("X-User-Id");
                headers.remove("X-User-Role");
                headers.remove("X-Gateway-Secret");
            }).build()).build();

        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .filter(auth -> auth != null && auth.getPrincipal() instanceof Jwt)
            .map(auth -> (Jwt) auth.getPrincipal())
            .flatMap(jwt -> {
                String userId = jwt.getSubject();
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                String roles = "";
                if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roleList) {
                    roles = roleList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role -> "ROLE_" + role.toUpperCase())
                        .collect(Collectors.joining(","));
                }

                ServerHttpRequest request = sanitized.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", roles)
                    .header("X-Gateway-Secret", internalSecret)
                    .build();
                return chain.filter(sanitized.mutate().request(request).build());
            })
            .switchIfEmpty(chain.filter(addGatewaySecret(sanitized)));
    }

    private ServerWebExchange addGatewaySecret(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
            .header("X-Gateway-Secret", internalSecret)
            .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
