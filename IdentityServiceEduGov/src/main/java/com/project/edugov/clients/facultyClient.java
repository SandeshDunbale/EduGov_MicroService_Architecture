package com.project.edugov.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.FacultyFeignDTO;

//Interface for communication with Faculty service

@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "facultyClient")
public interface facultyClient {

	// Find by Primary Key (Keep this if you use it elsewhere)
	@GetMapping("/faculty/{id}")
	FacultyFeignDTO getFacultyById(@PathVariable("id") Long id);

	// 🟢 NEW: Find by Foreign Key (User ID)
	@GetMapping("/faculty/user/{userId}")
	FacultyFeignDTO getFacultyByUserId(@PathVariable("userId") Long userId);
}