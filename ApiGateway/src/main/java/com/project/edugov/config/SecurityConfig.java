/*
 * package com.project.edugov.config;
 * 
 * import org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration; import
 * org.springframework.security.config.web.server.ServerHttpSecurity; import
 * org.springframework.security.web.server.SecurityWebFilterChain;
 * 
 * @Configuration public class SecurityConfig {
 * 
 * @Bean public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity
 * http) {
 * 
 * return http .csrf(ServerHttpSecurity.CsrfSpec::disable)
 * .authorizeExchange(exchange -> exchange .pathMatchers("/auth/**").permitAll()
 * .anyExchange().authenticated() ) .build(); } }
 */


package com.project.edugov.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
 
@Configuration
public class SecurityConfig {
 
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
 
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll()   // ✅ allow all
                )
                .build();
    }
}