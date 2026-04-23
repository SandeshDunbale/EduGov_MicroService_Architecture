package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication 
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.project.edugov.client")
public class ComplianceAndGovernanceServiceEduGovApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            ComplianceAndGovernanceServiceEduGovApplication.class, args
        );
    }
}