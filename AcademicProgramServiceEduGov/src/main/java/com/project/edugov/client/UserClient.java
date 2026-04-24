package com.project.edugov.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.UserFeignDTO;

//Interface for communication with Identity service.

@FeignClient(name = "IDENTITYSERVICEEDUGOV")
public interface UserClient {

	// Fetch UNIV_ADMIN details by ID
	@GetMapping("/api/users/{id}")
	UserFeignDTO getUserById(@PathVariable("id") Long id);
	
	@GetMapping("/api/users/role/{role}")
	List<UserFeignDTO> getUsersByRole(@PathVariable("role") String role);
}