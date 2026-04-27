package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
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
    
    @Component
    public class FeignClientInterceptor implements RequestInterceptor {
        private static final String AUTHORIZATION_HEADER = "Authorization";

        @Override
        public void apply(RequestTemplate requestTemplate) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String authHeader = request.getHeader(AUTHORIZATION_HEADER);
                if (authHeader != null) {
                    requestTemplate.header(AUTHORIZATION_HEADER, authHeader);
                }
            }
        }
    }
}