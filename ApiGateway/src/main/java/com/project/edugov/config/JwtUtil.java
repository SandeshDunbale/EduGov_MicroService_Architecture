/*
 * package com.project.edugov.config;
 * 
 * import io.jsonwebtoken.Jwts; import io.jsonwebtoken.SignatureAlgorithm;
 * import org.springframework.stereotype.Component;
 * 
 * import java.util.Date;
 * 
 * @Component public class JwtUtil {
 * 
 * private final String SECRET = "mysecretkey";
 * 
 * public String generateToken(String username) { return Jwts.builder()
 * .setSubject(username) .setIssuedAt(new Date()) .setExpiration(new
 * Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
 * .signWith(SignatureAlgorithm.HS256, SECRET) .compact(); } public void
 * validateToken(String token) { Jwts.parser() .setSigningKey("mysecretkey")
 * .parseClaimsJws(token); } }
 */