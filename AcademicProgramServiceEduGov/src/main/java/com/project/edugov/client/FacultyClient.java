package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.FacultyFeignDTO;

//Interface for communication with Registration (Faculty) service

@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "facultyClient")
public interface FacultyClient {

	// Faculty details by ID
	@GetMapping("/faculty/{id}")
	FacultyFeignDTO getFacultyById(@PathVariable("id") Long id);
}