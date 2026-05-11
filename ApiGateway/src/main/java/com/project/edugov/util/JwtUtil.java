package com.project.edugov.util;
 
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
 
import java.security.Key;
 
@Component
public class JwtUtil {
 
    public static final String SECRET = "413F4428472B4B6250655368566D5970337336763979244226452948404D6351";
 
    public void validateToken(final String token) {
        Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
    }
 
    public Claims getClaims(final String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
    }
 
    // OPTIONAL: Add this if your Gateway filter needs to extract the user ID
    // to pass it as a header to downstream microservices.
    public Long extractUserId(final String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }
 
    // OPTIONAL: Helper for extracting role
    public String extractRole(final String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }
 
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}