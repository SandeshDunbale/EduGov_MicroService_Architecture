package com.project.edugov.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
 
import java.util.Arrays;
 
@Configuration
public class CorsConfig {
 
    @Bean
    public CorsWebFilter corsWebFilter() {
 
        CorsConfiguration config = new CorsConfiguration();
 
        config.setAllowCredentials(true);
 
        config.setAllowedOrigins(
                Arrays.asList("http://localhost:3000")
        );
 
        config.setAllowedHeaders(
                Arrays.asList("*")
        );
 
        config.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
 
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
 
        source.registerCorsConfiguration("/**", config);
 
        return new CorsWebFilter(source);
    }
}
 