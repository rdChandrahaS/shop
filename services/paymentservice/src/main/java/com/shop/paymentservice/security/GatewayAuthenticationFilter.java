package com.shop.paymentservice.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
public class GatewayAuthenticationFilter extends OncePerRequestFilter{
	@Override
	protected void doFilterInternal(HttpServletRequest request, 
			HttpServletResponse response, 
			FilterChain filterChain)throws ServletException, IOException {
		
		
		String userId = request.getHeader("X-User-Id");
		String role = request.getHeader("X-User-Role");
		
		if(userId != null && !userId.isEmpty()) { // If the header exists, we trust the Gateway and tell Spring Security the user is authenticated
			
			List<SimpleGrantedAuthority> authorities = Collections.emptyList();
			
		    if (role != null && !role.isEmpty()) {
		        authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
		    }
		    
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userId, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(token);
		}
		filterChain.doFilter(request, response); // Continue the filter
	}

}
