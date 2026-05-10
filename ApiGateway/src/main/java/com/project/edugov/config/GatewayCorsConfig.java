package com.project.edugov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayCorsConfig {

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration config = new CorsConfiguration();

		// 1. Allow the React frontend port
		config.addAllowedOrigin("http://localhost:5173");

		// 2. Allow all standard headers (Authorization, Content-Type, etc.)
		config.addAllowedHeader("*");

		// 3. Allow all methods (GET, POST, PATCH, PUT, OPTIONS)
		config.addAllowedMethod("*");

		// 4. Allow credentials (important for cookies/auth)
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		// Apply this to every route in the gateway
		source.registerCorsConfiguration("/**", config);

		return new CorsWebFilter(source);
	}
}