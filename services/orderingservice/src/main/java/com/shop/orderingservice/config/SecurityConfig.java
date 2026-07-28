package com.shop.orderingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Disable CSRF so Postman/Browser requests aren't blocked
				.csrf(csrf -> csrf.disable())
				// Tell Spring to permit absolutely everything without a login
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}
}