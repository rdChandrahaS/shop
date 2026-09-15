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

    @SuppressWarnings("unchecked")
    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest sanitizedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Gateway-Secret");
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange
                .mutate()
                .request(sanitizedRequest)
                .build();

        return ReactiveSecurityContextHolder.getContext()
                .filter(context ->
                        context.getAuthentication() != null
                        && context.getAuthentication().getPrincipal() instanceof Jwt)
                .map(context ->
                        (Jwt) context.getAuthentication().getPrincipal())
                .flatMap(jwt -> {

                    String userId = jwt.getSubject();

                    Map<String, Object> realmAccess =
                            jwt.getClaimAsMap("realm_access");

                    String roles = "";

                    if (realmAccess != null
                            && realmAccess.containsKey("roles")) {

                        List<String> roleList =
                                (List<String>) realmAccess.get("roles");

                        roles = roleList.stream()
                                .map(role ->
                                        "ROLE_" + role.toUpperCase())
                                .collect(Collectors.joining(","));
                    }

                    ServerHttpRequest request =
                            sanitizedExchange.getRequest()
                                    .mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Role", roles)
                                    .header("X-Gateway-Secret", internalSecret)
                                    .build();

                    return chain.filter(
                            sanitizedExchange
                                    .mutate()
                                    .request(request)
                                    .build());
                })
                .switchIfEmpty(
                        chain.filter(sanitizedExchange));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}