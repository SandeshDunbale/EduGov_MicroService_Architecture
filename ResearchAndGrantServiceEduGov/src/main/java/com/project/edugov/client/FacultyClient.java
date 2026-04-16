package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.FacultyMinimalDTO;

// We assume the Identity/User team named their service "user-service" in Eureka
@FeignClient(name = "user-service", contextId = "facultyClient") 
public interface FacultyClient {

    // Fetches a single faculty member to validate them before creating a project
    @GetMapping("/faculty/{id}")
    FacultyMinimalDTO getFacultyById(@PathVariable("id") Long facultyId);
    
}