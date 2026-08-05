package com.shop.authservice.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {
	
	@Value("${hmac.sha.key}")
	private String secretKey;
	
	@Value("${jwt.session.expiry}")
	private long session;
	
	private Key key;
	
	@PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
	
	public String generateToken(String userId, String userName, Set<String> roles) {
		
		Map<String, Object> claims = new HashMap<>();
		Map<String, Object> realmAccess = new HashMap<>();
		long now = System.currentTimeMillis();
		
		realmAccess.put("roles", roles);
		claims.put("realm_access", realmAccess);
		claims.put("username", userName);
		
		return Jwts.builder()
				.claims(claims)
				.subject(userId)
				.issuedAt(new Date(now))
				.expiration(new Date(now + session))
				.signWith(key)
				.compact();
	}
}
