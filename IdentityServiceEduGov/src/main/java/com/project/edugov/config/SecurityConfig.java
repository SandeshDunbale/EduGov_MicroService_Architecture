package com.project.edugov.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.project.edugov.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 📍 FIX: YOU MUST CALL .cors() HERE to activate the bean below!
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/users/recoverEmail", "/api/identity/register").permitAll()
                .requestMatchers("/api/audit/internal/log").permitAll()
                .requestMatchers(
                	    "/api/resources/by-type/**",
                	    "/api/infrastructure/by-type/**",
                	    "/api/resources/by-type-program",
                	    "/api/infrastructure/by-type-program"

                	).permitAll()

                // INTERNAL USER FETCHING
                .requestMatchers("/api/users/role/**").hasAnyAuthority(
                    "UNIV_ADMIN", "ROLE_UNIV_ADMIN", 
                    "PROG_MANAGER", "ROLE_PROG_MANAGER"
                )
                

                // Match general user paths SECOND

                // Match general user paths SECOND   
                //added extra prog_manger for mod 4

                .requestMatchers("/api/users/**").hasAnyAuthority("UNIV_ADMIN", "ROLE_UNIV_ADMIN", "FACULTY", "ROLE_FACULTY", "STUDENT", "ROLE_STUDENT","PROG_MANAGER","ROLE_PROG_MANAGER")

                // Match general user paths
                .requestMatchers("/api/users/**").hasAnyAuthority(
                    "UNIV_ADMIN", "ROLE_UNIV_ADMIN", 
                    "FACULTY", "ROLE_FACULTY", 
                    "STUDENT", "ROLE_STUDENT",
                    "PROG_MANAGER", "ROLE_PROG_MANAGER",
                    "COMPLIANCE_OFFICER", "ROLE_COMPLIANCE_OFFICER", 
                    "GOVT_AUDITOR", "ROLE_GOVT_AUDITOR"              
                )

                // SECURE EVERYTHING ELSE
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
}