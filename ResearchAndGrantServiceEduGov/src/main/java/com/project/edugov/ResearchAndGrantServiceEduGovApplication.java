package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;


//@EnableDiscoveryClient // Tells this app to register itself with your Eureka Server (Port 8001)
@EnableFeignClients    // <-- Tells Spring to scan for your FacultyClient and UserClient
@SpringBootApplication
public class ResearchAndGrantServiceEduGovApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResearchAndGrantServiceEduGovApplication.class, args);
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
					System.out.println("✅ FEIGN SUCCESS: Token found and attached to request!");
					requestTemplate.header("Authorization", authHeader);
				} else {
					System.out.println("❌ FEIGN ERROR: The incoming request has NO Authorization header!");
				}
			} else {
				System.out.println("❌ FEIGN ERROR: Request Context is NULL! Thread lost the request.");
			}
		};
	}
}

