package com.project.edugov.filter;

import com.project.edugov.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

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

            if (validator.isSecured.test(exchange.getRequest())) {

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                if (authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }

                try {
                    // 1. Check if token is mathematically valid
                    jwtUtil.validateToken(authHeader);

                    // 2. Extract User Data
                    Claims claims = jwtUtil.getClaims(authHeader);
                    String role = claims.get("role", String.class);
                    String email = claims.getSubject();

                    // 3. ROLE-BASED ACCESS CONTROL (RBAC)
                    // If this specific route requires specific roles, check them now
                    if (config.getAllowedRoles() != null && !config.getAllowedRoles().isEmpty()) {
                        if (!config.getAllowedRoles().contains(role)) {
                            System.out.println("Access Denied: User " + email + " lacks required role.");
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }
                    }

                    // 4. Pass the user data to your teammates' microservices as headers
                    exchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Email", email)
                                    .header("X-User-Role", role)
                                    .build())
                            .build();

                } catch (Exception e) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            return chain.filter(exchange);
        };
    }

    // Now we use this class to pass specific roles from the GatewayConfig
    public static class Config {
        private List<String> allowedRoles;

        public Config() {}

        public List<String> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<String> allowedRoles) { this.allowedRoles = allowedRoles; }
    }
}