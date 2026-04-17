package com.project.edugov.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm; // <-- FIXED IMPORT
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    // IMPORTANT: In production, store this in application.properties!
    // This is a securely generated 256-bit key required by HS256.
    private static final String SECRET = "413F4428472B4B6250655368566D5970337336763979244226452948404D6351";
    
    // Token validity: 24 Hours
    private static final long JWT_TOKEN_VALIDITY = 1000 * 60 * 60 * 24;

    private Key getSignKey() {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. Extract Email (Username) from Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 2. Extract Expiration Date
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 3. Generate Token (Called upon successful login)
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // Store the role inside the token
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 4. Validate Token (Called on every request)
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}