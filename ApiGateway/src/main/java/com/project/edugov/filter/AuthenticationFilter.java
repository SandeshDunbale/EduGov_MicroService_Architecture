package com.project.edugov.filter;

import com.project.edugov.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 📍 PERMIT OPTIONS: Allow preflight requests to pass without a token
            if (org.springframework.http.HttpMethod.OPTIONS.name().equals(exchange.getRequest().getMethod().name())) {
                return chain.filter(exchange);
            }

            if (validator.isSecured.test(exchange.getRequest())) {
                // 2. Check for the Authorization Header
                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || authHeader.isEmpty()) {
                    System.out.println("AUTH FAIL: Authorization header is missing.");
                    return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                // 3. Extract and clean the token
                String token = authHeader;
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7).trim(); 
                }

                try {
                    // 4. Validate the token mathematically
                    jwtUtil.validateToken(token);

                    // 5. Extract user data (Role, Email, and ID)
                    Claims claims = jwtUtil.getClaims(token);
                    String role = claims.get("role") != null ? claims.get("role").toString() : "";
                    String cleanRole = role.replace("ROLE_", ""); // Clean prefix
                    String email = claims.getSubject();
                    String userId = claims.get("userId") != null ? claims.get("userId").toString() : "";

                    System.out.println("AUTH SUCCESS: " + email + " (ID: " + userId + ") with role " + cleanRole);

                    // 6. Role-Based Access Control (RBAC)
                    if (config.getAllowedRoles() != null && !config.getAllowedRoles().isEmpty()) {
                        if (!config.getAllowedRoles().contains(cleanRole) && !config.getAllowedRoles().contains(role)) {
                            System.out.println("AUTH FAIL: Role " + role + " is not authorized for this route.");
                            return onError(exchange, "Unauthorized Role", HttpStatus.FORBIDDEN);
                        }
                    }

                    // 7. Pass user data to downstream services via headers
                    exchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Email", email)
                                    .header("X-User-Role", cleanRole)
                                    .header("X-User-Id", userId) 
                                    .build())
                            .build();

                } catch (Exception e) {
                    System.err.println("JWT VALIDATION ERROR: " + e.getMessage());
                    return onError(exchange, "Invalid or Expired Token", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange);
        };
    }

    // Helper method to handle error responses
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        private List<String> allowedRoles;
        
       //newly added
        public Config(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public Config() {}

        public List<String> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<String> allowedRoles) { this.allowedRoles = allowedRoles; }
    }
}