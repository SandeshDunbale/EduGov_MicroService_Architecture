package com.project.edugov.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayCorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
    	CorsConfiguration corsConfig = new CorsConfiguration();
        // Allow your frontend
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*")); // Use wildcard for headers to avoid mismatches [cite: 166]
        corsConfig.setAllowCredentials(true);        
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 📍 FIX: Because allowCredentials is true, we CANNOT use "*". We must explicitly list them.
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "X-Requested-With", 
            "x-requested-with",
            "X-User-Id", 
            "x-user-id", 
            "X-User-Role", 
            "X-User-Email"
        ));
        corsConfig.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this to ALL paths (/**) to cover every microservice route [cite: 167]
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }
}