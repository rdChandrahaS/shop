package com.shop.apigatewayservice.config;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> {})
            .authorizeExchange(exchange -> exchange
				.pathMatchers("/auth/register", "/auth/login").permitAll()

				.pathMatchers(HttpMethod.GET, "/foods", "/foods/**").permitAll()
				.pathMatchers(HttpMethod.POST, "/foods", "/foods/**").hasRole("ADMIN")
				.pathMatchers(HttpMethod.PUT, "/foods", "/foods/**").hasRole("ADMIN")
				.pathMatchers(HttpMethod.DELETE, "/foods", "/foods/**").hasRole("ADMIN")

				.pathMatchers("/payment/webhook").permitAll()
				.pathMatchers("/actuator/health", "/actuator/info", "/favicon.ico").permitAll()

				.pathMatchers("/order").hasRole("USER")
				.pathMatchers("/order/*/status").hasAnyRole("USER", "ADMIN")
				.pathMatchers("/order/**").authenticated()

				.pathMatchers("/payment/**").authenticated()

				.anyExchange().authenticated()
			)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }

    @Bean
	public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
		return jwt -> {
			Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
			Collection<SimpleGrantedAuthority> authorities = List.of();

			if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
				authorities = roles.stream()
						.filter(String.class::isInstance)
						.map(String.class::cast)
						.map(String::toUpperCase)
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
						.collect(Collectors.toList());
			}

			return Mono.just(new JwtAuthenticationToken(jwt, authorities));
		};
	}

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256");
        }
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public ApplicationRunner printRoutes(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes()
            .doOnNext(route -> System.out.println("GATEWAY ROUTE: " + route.getId() + " -> " + route.getUri() + " " + route.getPredicate()))
            .subscribe();
    }
}
