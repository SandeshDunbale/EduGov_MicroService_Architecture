package com.project.edugov;
 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
 
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationsServiceEduGovApplication {
 
    public static void main(String[] args) {
        SpringApplication.run(NotificationsServiceEduGovApplication.class, args);
    }
}
