package com.project.edugov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;


//@EnableDiscoveryClient // Tells this app to register itself with your Eureka Server (Port 8001)
@EnableFeignClients    // <-- Tells Spring to scan for your FacultyClient and UserClient
@SpringBootApplication
public class ResearchAndGrantServiceEduGovApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResearchAndGrantServiceEduGovApplication.class, args);
	}

}
