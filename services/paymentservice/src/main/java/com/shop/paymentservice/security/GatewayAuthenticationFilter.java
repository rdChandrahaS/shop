package com.shop.paymentservice.security;

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
public class GatewayAuthenticationFilter extends OncePerRequestFilter{

    @Value("${gateway.internal.secret}")
    private String gatewayInternalSecret;
	@Override
	protected void doFilterInternal(HttpServletRequest request, 
			HttpServletResponse response, 
			FilterChain filterChain)throws ServletException, IOException {
		
		
        // Razorpay calls the webhook directly, so it must not be forced through the gateway-secret check.
        if ("/payment/webhook".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

		String gatewaySecret = request.getHeader("X-Gateway-Secret");
		if (gatewayInternalSecret == null || !gatewayInternalSecret.equals(gatewaySecret)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Requests must pass through the API gateway");
			return;
		}

		String userId = request.getHeader("X-User-Id");
		String role = request.getHeader("X-User-Role");
		
		if(userId != null && !userId.isEmpty()) { // If the header exists, we trust the Gateway and tell Spring Security the user is authenticated
			
			List<SimpleGrantedAuthority> authorities = Collections.emptyList();
			
		    if (role != null && !role.isEmpty()) {
		        authorities = Arrays.stream(role.split(","))
		        					.map(String::trim)
		        					.filter(r -> !r.isEmpty())
		        					.map(SimpleGrantedAuthority::new)
		        					.collect(Collectors.toList());
		    }
		    
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userId, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(token);
		}
		filterChain.doFilter(request, response); // Continue the filter
	}

}
