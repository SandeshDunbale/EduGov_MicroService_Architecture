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
                // Route for the Identity Service
                .route("identity-service-route", r -> r
                        .path("/api/auth/**")
                        .uri("lb://IDENTITYSERVICEEDUGOV"))
                .build();
    }
}