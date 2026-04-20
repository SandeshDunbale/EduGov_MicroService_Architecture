package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.FacultyFeignDTO;

/**
 * Feign Client to communicate with the FACULTY-SERVICE (FacultyController).
 */
@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "facultyClient")
public interface FacultyClient {

	/**
	 * Calls: GET http://FACULTY-SERVICE/faculty/{id} This matches your
	 * FacultyController @GetMapping("/{id}")
	 */
	@GetMapping("/faculty/{id}")
	FacultyFeignDTO getFacultyById(@PathVariable("id") Long id);
}