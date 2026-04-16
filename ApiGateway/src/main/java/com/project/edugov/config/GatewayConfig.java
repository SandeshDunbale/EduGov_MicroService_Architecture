package com.project.edugov.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity-service-route", r -> r
                        // UPDATE: Added /api/users/** to the allowed paths
                        .path("/api/auth/**", "/api/users/**") 
                        .uri("lb://IDENTITYSERVICEEDUGOV"))
                .build();
    }
}