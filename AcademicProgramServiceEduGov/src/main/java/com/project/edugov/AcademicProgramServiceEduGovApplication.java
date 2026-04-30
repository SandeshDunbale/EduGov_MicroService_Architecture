package com.project.edugov;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.project.edugov.client") // Ensure this covers the fallback package
@ComponentScan(basePackages = "com.project.edugov")      // Ensure this scans the @Component
public class AcademicProgramServiceEduGovApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcademicProgramServiceEduGovApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();

		// Strict matching prevents ambiguous ID mapping across microservices
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		return modelMapper;
	}
	@Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // Grab the token from the incoming Gateway request
                String authHeader = request.getHeader("Authorization");
                
                // Attach it to the outgoing Feign request
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
