package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;


@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient//optional
@EnableAsync
public class ResourceAndInfrastructureServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceAndInfrastructureServiceApplication.class, args);
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
            }else {
                // IF YOU SEE THIS IN LOGS, the token is NOT being passed to Identity Service
                System.out.println("DEBUG: RequestContextHolder is null! Token propagation failed.");
            }
        };
    }
}

