/*
 * package com.project.edugov.config;
 * 
 * import org.springframework.cloud.gateway.filter.GlobalFilter; import
 * org.springframework.cloud.gateway.filter.GatewayFilterChain; import
 * org.springframework.http.HttpHeaders; import
 * org.springframework.stereotype.Component; import
 * org.springframework.web.server.ServerWebExchange;
 * 
 * import reactor.core.publisher.Mono;
 * 
 * @Component public class JwtFilter implements GlobalFilter {
 * 
 * private final JwtUtil jwtUtil;
 * 
 * public JwtFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }
 * 
 * @Override public Mono<Void> filter(ServerWebExchange exchange,
 * GatewayFilterChain chain) {
 * 
 * String path = exchange.getRequest().getURI().getPath(); String method =
 * exchange.getRequest().getMethod().name();
 * 
 * System.out.println("Incoming Request -> Path: " + path + " | Method: " +
 * method);
 * 
 * // ✅ Allow AUTH APIs (no token required) if (path.contains("/auth")) { return
 * chain.filter(exchange); }
 * 
 * // ✅ Allow GET /api/reports without token if (path.startsWith("/api/reports")
 * && method.equals("GET")) { return chain.filter(exchange); }
 * 
 * // 🔐 Validate JWT Token String authHeader = exchange.getRequest()
 * .getHeaders() .getFirst(HttpHeaders.AUTHORIZATION);
 * 
 * if (authHeader == null || !authHeader.startsWith("Bearer ")) { throw new
 * RuntimeException("Missing or Invalid Authorization Header"); }
 * 
 * String token = authHeader.substring(7);
 * 
 * try { jwtUtil.validateToken(token); } catch (Exception e) { throw new
 * RuntimeException("Invalid or Expired Token"); }
 * 
 * return chain.filter(exchange); } }
 */