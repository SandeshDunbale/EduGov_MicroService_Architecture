package com.project.edugov.config;

import com.project.edugov.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authFilter;

    public GatewayConfig(AuthenticationFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1. PUBLIC ROUTES (No Auth Filter)
                .route("public-auth", r -> r
                        .path("/api/identity/register", "/api/auth/**")
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // 2. DOCUMENT SERVICE (Basic Auth)
                .route("document-service-route", r -> r
                        .path("/documents/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config()))) // Fixed parens
                        .uri("lb://DOCUMENT-SERVICE"))

                // 3. IDENTITY ADMIN (Role Based)
                .route("identity-admin-update", r -> r
                        .path("/api/users/status/**")
                        .and().method("PATCH")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // 4. IDENTITY MANAGER (Role Based)
                .route("identity-manager-view", r -> r
                        .path("/api/users/role/**", "/api/users/status/**")
                        .and().method("GET")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN", "PROG_MANAGER"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // 5. IDENTITY GENERAL (Catch-all with Basic Auth)
                .route("identity-general", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config()))) // Fixed parens
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // 6. REGISTRATION SERVICE
                .route("registration-service-route", r -> r
                        .path("/students/**", "/faculty/**")
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                // 7. RESEARCH & GRANTS
                .route("research-project-route", r -> r
                        .path("/api/projects/**", "/api/grants/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config()))) // Fixed parens
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))

                // 8. ACADEMIC SERVICE
                .route("academic-service-route", r -> r
                        .path("/programs/**", "/courses/**", "/enrollments/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN", "STUDENT", "FACULTY"));
                            return f.filter(authFilter.apply(config));
                        }) // Fixed closing brace and paren
                        .uri("lb://ACADEMIC-SERVICE"))
                .build();
    }
}