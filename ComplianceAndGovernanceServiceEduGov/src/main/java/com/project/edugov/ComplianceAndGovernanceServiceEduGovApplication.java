package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;


import org.springframework.context.annotation.Bean;


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
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    
    }
}