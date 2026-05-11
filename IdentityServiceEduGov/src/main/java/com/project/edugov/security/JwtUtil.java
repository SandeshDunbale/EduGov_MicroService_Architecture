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
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private static final String SECRET = "413F4428472B4B6250655368566D5970337336763979244226452948404D6351";
    private static final long JWT_TOKEN_VALIDITY = 1000 * 60 * 60 * 24;

    private Key getSignKey() {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Long extractUserId(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class); 
    }

    // 🟢 NEW: Extract Faculty ID from Token
    public Long extractFacultyId(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("facultyId", Long.class); 
    }

    // 🟢 NEW: Extract Student ID from Token
    public Long extractStudentId(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("studentId", Long.class); 
    }

    public String extractRole(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
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

    // 🟢 UPDATED: Generate Token now accepts facultyId and studentId
    public String generateToken(String email, String role, Long userId, Long facultyId, Long studentId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); 
        claims.put("userId", userId);
        
        // Only put them in the token if they actually exist!
        if (facultyId != null) {
            claims.put("facultyId", facultyId);
        }
        if (studentId != null) {
            claims.put("studentId", studentId);
        }
        
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

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}