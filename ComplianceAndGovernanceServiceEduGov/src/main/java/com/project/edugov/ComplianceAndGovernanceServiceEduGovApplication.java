package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;

@SpringBootApplication 
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.project.edugov.client")
public class ComplianceAndGovernanceServiceEduGovApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            ComplianceAndGovernanceServiceEduGovApplication.class, args
        );
    }
    
    @Bean
    public feign.RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}