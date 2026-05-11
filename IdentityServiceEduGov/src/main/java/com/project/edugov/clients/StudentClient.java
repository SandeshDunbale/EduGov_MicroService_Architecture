package com.project.edugov.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.StudentFeignDTO;

@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "studentClient")
public interface StudentClient {

	// Existing: Fetch Student details by Primary Key
	@GetMapping("/students/{id}")
	StudentFeignDTO getStudentById(@PathVariable("id") Long id);
	
	// 🟢 NEW: Fetch Student details by Foreign Key (User ID)
	@GetMapping("/students/user/{userId}")
	StudentFeignDTO getStudentByUserId(@PathVariable("userId") Long userId);
}