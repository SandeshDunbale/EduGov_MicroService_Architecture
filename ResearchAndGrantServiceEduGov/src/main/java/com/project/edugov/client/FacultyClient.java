package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.FacultyMinimalDTO;

// We assume the Identity/User team named their service "user-service" in Eureka
//1. Point to the Registration Service in Eureka
@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "facultyClient") 
public interface FacultyClient {

 // 2. Match the exact URL from the controller you just pasted (NO /api)
 @GetMapping("/faculty/{id}") 
 FacultyMinimalDTO getFacultyById(@PathVariable("id") Long facultyId);
}