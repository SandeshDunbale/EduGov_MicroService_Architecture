package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.StudentFeignDTO;

/**
 * Feign Client to communicate with the STUDENT-SERVICE (StudentController).
 */
@FeignClient(name = "REGISTRATIONSERVICEEDUGOV", contextId = "studentClient")
public interface StudentClient {

	/**
	 * Calls: GET http://STUDENT-SERVICE/students/{id} This matches your
	 * StudentController @GetMapping("/{id}")
	 */
	@GetMapping("/students/{id}")
	StudentFeignDTO getStudentById(@PathVariable("id") Long id);
}