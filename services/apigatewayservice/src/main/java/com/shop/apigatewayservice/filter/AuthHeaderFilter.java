package com.shop.apigatewayservice.filter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class AuthHeaderFilter implements GlobalFilter, Ordered{
	
	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return ReactiveSecurityContextHolder.getContext()
				.filter(c -> c.getAuthentication() != null && c.getAuthentication().getPrincipal() instanceof Jwt)
				.map(c -> (Jwt) c.getAuthentication().getPrincipal())
				.flatMap(jwt -> {
					String userId = jwt.getSubject();
					Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
					String roles = "";
					if(realmAccess != null && realmAccess.containsKey("roles")) {
						List<String> roleList = (List<String>) realmAccess.get("roles");
						roles = roleList.stream()
										.map(role -> "ROLE_" + role.toUpperCase())
										.collect(Collectors.joining(","));
					}
					
					ServerHttpRequest request = exchange.getRequest()
														.mutate()
														.header("X-User-Id", userId)
								                        .header("X-User-Role", roles)
								                        .build();
					
					return chain.filter(exchange.mutate().request(request).build());
				})
				.switchIfEmpty(chain.filter(exchange));
	}
	
	@Override
	public int getOrder() {
		return 0;
	}
}
