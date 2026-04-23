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

                // ==========================================
                // 1. PUBLIC ROUTES (No Auth Filter)
                // ==========================================
                .route("public-auth", r -> r
                        .path("/api/auth/**", "/api/users/login", "/api/users/recoverEmail", "/api/users/resetPassword", "/students/register", "/faculty/register")
                        .uri("lb://IDENTITYSERVICEEDUGOV")) // Assuming registration hits identity first

                // ==========================================
                // 2. RESEARCH AND GRANT SERVICE
                // ==========================================
                // Faculty creating/updating projects
                .route("research-faculty-modify", r -> r
                        .method("POST", "PUT").and().path("/api/projects/**", "/api/grants/apply/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("FACULTY"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))
                
                // Manager/Admin Grant Actions
                .route("research-admin-actions", r -> r
                        .method("POST").and().path("/api/grants/decision/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("PROG_MANAGER", "UNIV_ADMIN", "GOVT_AUDITOR"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))

                // General View Access for Projects/Grants
                .route("research-general-view", r -> r
                        .method("GET").and().path("/api/projects/**", "/api/grants/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("FACULTY", "PROG_MANAGER", "UNIV_ADMIN", "GOVT_AUDITOR"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))

                // ==========================================
                // 3. REGISTRATION SERVICE
                // ==========================================
                // Faculty/Student self-updates
                .route("registration-self-update-student", r -> r
                        .method("PUT").and().path("/students/*/update")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("STUDENT", "GOVT_AUDITOR"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                // Admin Management
                .route("registration-admin-manage", r -> r
                        .path("/students/**", "/faculty/**") // Catch all other methods (GET, PATCH, DELETE)
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN", "PROG_MANAGER", "GOVT_AUDITOR"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://REGISTRATIONSERVICEEDUGOV"))

                // ==========================================
                // 4. ACADEMIC PROGRAM SERVICE
                // ==========================================
                // Admin creates courses/programs
                .route("academic-admin-modify", r -> r
                        .method("POST", "PATCH").and().path("/programs/**", "/courses/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://ACADEMICPROGRAMSERVICEEDUGOV"))
                
                // General viewing
                .route("academic-general-view", r -> r
                        .method("GET").and().path("/programs/**", "/courses/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN", "STUDENT", "FACULTY"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://ACADEMICPROGRAMSERVICEEDUGOV"))

                // ==========================================
                // 5. COMPLIANCE & GOVERNANCE SERVICE
                // ==========================================
                .route("compliance-officer-modify", r -> r
                        .method("POST", "PUT", "DELETE").and().path("/api/audits/**", "/api/compliance/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("COMPLIANCE_OFFICER"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://COMPLIANCEANDGOVERNANCESERVICEEDUGOV"))

                .route("compliance-auditor-review", r -> r
                        .method("PATCH").and().path("/api/audits/*/review")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("GOVT_AUDITOR"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://COMPLIANCEANDGOVERNANCESERVICEEDUGOV"))

                .route("compliance-general-view", r -> r
                        .method("GET").and().path("/api/audits/**", "/api/compliance/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("COMPLIANCE_OFFICER", "GOVT_AUDITOR"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://COMPLIANCEANDGOVERNANCESERVICEEDUGOV"))
                
                
             // ==========================================
                // 6. RESOURCE & INFRASTRUCTURE SERVICE
                // ==========================================
                // Students requesting resources
                .route("resource-student-request", r -> r
                        .method("POST").and().path("/api/requests/resource")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("STUDENT"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESOURCEANDINFRASTRUCTURESERVICE"))

                // Faculty requesting infrastructure
                .route("resource-faculty-request", r -> r
                        .method("POST").and().path("/api/requests/infrastructure")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("FACULTY"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESOURCEANDINFRASTRUCTURESERVICE"))

                // Managers approving/declining requests
                .route("resource-manager-approval", r -> r
                        .method("POST").and().path("/api/requests/*/approve", "/api/requests/*/decline")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("PROG_MANAGER"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESOURCEANDINFRASTRUCTURESERVICE"))

                // Managers managing the actual infrastructure
                .route("infrastructure-management", r -> r
                        .path("/api/infrastructure/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("PROG_MANAGER"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://RESOURCEANDINFRASTRUCTURESERVICE"))

                // General view for requests (Authenticated users only)
                .route("resource-general-view", r -> r
                        .method("GET").and().path("/api/requests/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config()))) // No specific roles, just must be logged in
                        .uri("lb://RESOURCEANDINFRASTRUCTURESERVICE"))
                
                
             // ==========================================
                // 7. REPORTING & ANALYTICS SERVICE
             // ==========================================
                .route("reporting-analytics-route", r -> r
                        .path("/api/reports/**")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN", "PROG_MANAGER", "GOVT_AUDITOR", "COMPLIANCE_OFFICER"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://REPORTINGANDANALYTICSSERVICEEDUGOV"))
                
                
             // ==========================================
                // 8. NOTIFICATION SERVICE
             // ==========================================
                .route("notification-service-route", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config()))) 
                        .uri("lb://NOTIFICATIONSSERVICEEDUGOV"))
                
                
                .build();
    }
}