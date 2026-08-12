package com.shop.apigatewayservice.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
	
	@Value("${jwt.secret}")
    private String jwtSecret;
	
	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		
		http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(exchange -> exchange
					.pathMatchers(
							"/auth/**",
							"/foods/**"
					).permitAll()
					.anyExchange()
					.authenticated()
			).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));

			
		return http.build();
	}
	
	@Bean
	public ReactiveJwtDecoder jwtDecoder() {
		byte[] keyBytes = jwtSecret.getBytes();
		SecretKeySpec secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
		return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
	}
}
