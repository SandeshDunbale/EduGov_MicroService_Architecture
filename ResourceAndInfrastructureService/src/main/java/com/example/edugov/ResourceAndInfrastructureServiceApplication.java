package com.example.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients(basePackages = "com.example.edugov.feign")
@EnableDiscoveryClient//optional
public class ResourceAndInfrastructureServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceAndInfrastructureServiceApplication.class, args);
    }
}

