package com.project.edugov.config;

import com.project.edugov.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
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
                // 1. PUBLIC ROUTES
                .route("public-auth", r -> r
                        .path("/api/auth/**", "/api/users/login", "/api/users/recoverEmail", "/api/users/resetPassword", "/api/identity/register")
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // 2. RESEARCH AND GRANT SERVICE
                .route("research-faculty-modify", r -> r
                        .method("POST", "PUT").and().path("/api/projects/**", "/api/grants/apply/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("FACULTY")))))
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))

                .route("research-admin-actions", r -> r
                        .method("POST").and().path("/api/grants/decision/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("PROG_MANAGER", "UNIV_ADMIN", "GOVT_AUDITOR")))))
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))

                .route("research-general-view", r -> r
                        .method("GET").and().path("/api/projects/**", "/api/grants/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("FACULTY", "PROG_MANAGER", "UNIV_ADMIN", "GOVT_AUDITOR")))))
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))

                // 3. REGISTRATION SERVICE
                .route("registration-public", r -> r
                        .path("/students/register", "/faculty/register")
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                .route("registration-self-update", r -> r
                        .method("PUT").and().path("/students/*/update", "/faculty/*/update")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT", "FACULTY", "UNIV_ADMIN")))))
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                .route("registration-admin-manage", r -> r
                        .path("/students/status/**", "/students/all", "/students/*/approve", "/students/*/decline", "/students/*/delete", "/faculty/status/**", "/faculty/*/approve", "/faculty/*/decline", "/faculty/*/delete")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("UNIV_ADMIN", "PROG_MANAGER")))))
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                .route("registration-general-view", r -> r
                        .method("GET").and().path("/students/**", "/faculty/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT", "FACULTY", "UNIV_ADMIN", "PROG_MANAGER", "GOVT_AUDITOR")))))
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                // 4. ACADEMIC PROGRAM SERVICE
                .route("academic-admin-modify", r -> r
                        .method("POST", "PATCH", "PUT").and().path("/programs/**", "/courses/**", "/enrollments/update/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("UNIV_ADMIN")))))
                        .uri("lb://ACADEMICPROGRAMSERVICEEDUGOV"))
                
                
             // Add this under your Academic Service section
                .route("academic-student-view-enrollments", r -> r
                    .method("GET").and().path("/enrollments/user/**")
                    .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT", "UNIV_ADMIN")))))
                    .uri("lb://ACADEMICPROGRAMSERVICEEDUGOV"))

                .route("academic-student-enroll", r -> r
                        .method("POST").and().path("/enrollments/apply/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT")))))
                        .uri("lb://ACADEMICPROGRAMSERVICEEDUGOV"))

                .route("academic-general-view", r -> r
                        .method("GET").and().path("/programs/**", "/courses/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("UNIV_ADMIN", "STUDENT", "FACULTY")))))
                        .uri("lb://ACADEMICPROGRAMSERVICEEDUGOV"))

                // 5. COMPLIANCE SERVICE
                .route("compliance-officer-modify", r -> r
                        .method("POST", "PUT", "DELETE").and().path("/api/audits/**", "/api/compliance/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("COMPLIANCE_OFFICER")))))
                        .uri("lb://COMPLIANCEANDGOVERNANCESERVICEEDUGOV"))

                // 6. RESOURCE SERVICE
                .route("resource-student-request", r -> r
                        .method("POST").and().path("/api/requests/resource")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT")))))
                        .uri("lb://RESOURCEANDINFRASTRUCTURESERVICE"))

                // 7. REPORTING SERVICE
                .route("reporting-analytics-route", r -> r
                        .path("/api/reports/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("UNIV_ADMIN", "PROG_MANAGER", "GOVT_AUDITOR", "COMPLIANCE_OFFICER")))))
                        .uri("lb://REPORTINGANDANALYTICSSERVICEEDUGOV"))

                // 8. NOTIFICATION SERVICE
                .route("notification-service-route", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config()))) 
                        .uri("lb://NOTIFICATIONSSERVICEEDUGOV"))

                
                
               
                
                
                // 9. DOCUMENT SERVICE
                .route("document-upload-route", r -> r
                        .method("POST").and().path("/api/documents/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT", "FACULTY", "UNIV_ADMIN")))))
                        .uri("lb://DOCUMENT-SERVICE"))
             // ... existing code ...

             
                
                
                
                
                
             // 9. DOCUMENT SERVICE
             // 1. Specific route for file viewing (No filter for browser access)
                .route("document-view-route", r -> r
                        .path("/api/documents/file/**")
                        .uri("lb://DOCUMENT-SERVICE"))

                // 2. General route for API actions (Upload/Verify/List)
                .route("document-api-route", r -> r
                        .path("/api/documents/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config(List.of("STUDENT", "FACULTY", "UNIV_ADMIN")))))
                        .uri("lb://DOCUMENT-SERVICE"))
             .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:3000"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}