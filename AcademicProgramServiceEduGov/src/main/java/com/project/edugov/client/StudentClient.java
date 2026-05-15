package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.StudentFeignDTO;

//Interface for communication with Registration (Student) service

@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "studentClient")
public interface StudentClient {

	// Fetch Student details by ID
	@GetMapping("/students/{id}")
	StudentFeignDTO getStudentById(@PathVariable("id") Long id);
}