package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration; // milliseconds

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // milliseconds

    /**
     * Generate access token for the given user.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = buildClaims(user);
        return buildToken(claims, user.getPhone(), accessTokenExpiration);
    }

    /**
     * Generate refresh token for the given user.
     */
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getPhone(), refreshTokenExpiration);
    }

    /**
     * Extract the phone (subject) from the token.
     */
    public String extractPhone(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Check if the token is still valid (not expired and subject matches).
     */
    public boolean isTokenValid(String token, String phone) {
        try {
            final String extractedPhone = extractPhone(token);
            return extractedPhone.equals(phone) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Return the access token expiration in seconds (used in LoginResponse).
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("phone", user.getPhone());
        return claims;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
