package com.project.edugov;

import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;
import com.project.edugov.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class IdentityServiceEduGovApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceEduGovApplication.class, args);
    }

    // THIS SEEDS A TEST USER ON STARTUP SO YOU CAN TEST THE LOGIN API
    @Bean
    CommandLineRunner run(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // It checks if the user exists first so it doesn't crash on restart
            if (!userRepository.existsByEmail("test@edugov.com")) {
                User testUser = new User();
                testUser.setName("Test User");
                testUser.setEmail("test@edugov.com");
                testUser.setPhone("1234567890");
                testUser.setPasswordHash(passwordEncoder.encode("password123")); // Securely hashes the password
                testUser.setRole(Role.STUDENT);
                testUser.setStatus(Status.ACTIVE); // Must be active to bypass your login checks
                
                userRepository.save(testUser);
                System.out.println("✅ Test user successfully created in DB: test@edugov.com / password123");
            } else {
                System.out.println("✅ Test user already exists in DB.");
            }
        };
    }
    
}