package com.shop.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.shop.paymentservice.security.GatewayAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
	
	private final GatewayAuthenticationFilter gatewayAuthenticationFilter;
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) {
		
		http
			.csrf(AbstractHttpConfigurer::disable) // Disable CSRF because we are not using browser cookies/sessions
			.sessionManagement(session -> session.sessionCreationPolicy(null)) // Make the application completely stateless
			.authorizeHttpRequests(auth -> auth
	                .requestMatchers("/payment/webhook").permitAll() // 1. MUST BE PUBLIC: Razorpay needs to hit this without a JWT
	              	.requestMatchers("/payment/process").authenticated() // 2. MUST BE SECURED: Only internal Gateway traffic should hit this
	                .anyRequest().authenticated()  // 3. Lock down anything else just in case
	        )
			.addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
}
