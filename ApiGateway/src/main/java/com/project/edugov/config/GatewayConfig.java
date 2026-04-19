package com.project.edugov.config;

import com.project.edugov.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GatewayConfig {


//	@Bean
//	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//	    return builder.routes()
//	            // Route for Identity Service
//	            .route("identity-service-route", r -> r
//	                    .path("/api/auth/**", "/api/users/**", "/api/identity/**") 
//	                    .uri("lb://IDENTITYSERVICEEDUGOV"))
//	            
//	            // NEW: Route for Registration Service
//	            .route("registration-service-route", r -> r
//	                    .path("/students/**", "/faculty/**") 
//	                    .uri("lb://REGISTRATIONSERVICEEDUGOV")) // Use the ID from Eureka
//	            .build();
//	}

    private final AuthenticationFilter authFilter;

    public GatewayConfig(AuthenticationFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ----------------------------------------------------
                // 1. STRICTEST RULES FIRST: Admin Only Actions
                // ----------------------------------------------------
                // Only UNIV_ADMIN can PATCH (update) a user's status
                .route("identity-admin-update", r -> r
                        .path("/api/users/status/**")
                        .and().method("PATCH")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // ----------------------------------------------------
                // 2. MID-LEVEL RULES: Admin & Manager Views
                // ----------------------------------------------------
                // UNIV_ADMIN and PROG_MANAGER can GET users by role or status
                .route("identity-manager-view", r -> r
                        .path("/api/users/role/**", "/api/users/status/**")
                        .and().method("GET")
                        .filters(f -> {
                            AuthenticationFilter.Config config = new AuthenticationFilter.Config();
                            config.setAllowedRoles(List.of("UNIV_ADMIN", "PROG_MANAGER"));
                            return f.filter(authFilter.apply(config));
                        })
                        .uri("lb://IDENTITYSERVICEEDUGOV"))

                // ----------------------------------------------------
                // 3. GENERAL RULES: Catch-all for Identity Service
                // ----------------------------------------------------
                // Everything else (Login, Reset Password, Get User By ID) 
                // falls down to this generic route. No specific roles required.
                .route("identity-general", r -> r
                        .path("/api/auth/**", "/api/users/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://IDENTITYSERVICEEDUGOV"))
                
                .route("identity-service-route", r -> r
	                    .path("/api/auth/**", "/api/users/**", "/api/identity/**") 
	                    .uri("lb://IDENTITYSERVICEEDUGOV"))
	            
	            // NEW: Route for Registration Service
	            .route("registration-service-route", r -> r
	                    .path("/students/**", "/faculty/**") 
	                    .uri("lb://REGISTRATIONSERVICEEDUGOV")) // Use the ID from Eureka
	            
	            .route("research-project-route", r -> r
                        .path("/api/projects/**", "/api/grants/**")
                        // Assuming you want basic token validation for these endpoints:
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://RESEARCHANDGRANTSERVICEEDUGOV"))
                .build();
    }
}