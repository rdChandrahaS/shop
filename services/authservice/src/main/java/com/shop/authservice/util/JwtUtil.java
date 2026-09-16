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

    @Value("${jwt.session.expiry:3600000}")
    private long sessionExpiryMs;

    private Key key;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long");
        }
        if (sessionExpiryMs <= 0) {
            throw new IllegalStateException("jwt.session.expiry must be greater than zero");
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String userName, Set<String> roles) {
        long now = System.currentTimeMillis();

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);

        return Jwts.builder()
                .claim("realm_access", realmAccess)
                .claim("username", userName)
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + sessionExpiryMs))
                .signWith(key)
                .compact();
    }
}
