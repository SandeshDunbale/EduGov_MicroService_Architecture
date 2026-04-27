package com.project.edugov.config;

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

import com.project.edugov.security.JwtAuthenticationFilter;
//import feign.Request.HttpMethod;

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
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                
                // 1. PUBLIC ENDPOINTS
                .requestMatchers("/api/auth/**", "/api/users/recoverEmail", "/api/identity/register").permitAll()

                // 2. INTERNAL USER FETCHING (Order matters!)
                // Match specific sub-paths FIRST
                .requestMatchers("/api/users/role/**").hasAnyAuthority("UNIV_ADMIN", "ROLE_UNIV_ADMIN", "PROG_MANAGER", "ROLE_PROG_MANAGER")
                
                // Match general user paths SECOND
                .requestMatchers("/api/users/**").hasAnyAuthority("UNIV_ADMIN", "ROLE_UNIV_ADMIN", "FACULTY", "ROLE_FACULTY", "STUDENT", "ROLE_STUDENT","COMPLIANCE_OFFICER","ROLE_COMPLIANCE_OFFICER")

                // 3. SECURE EVERYTHING ELSE
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}