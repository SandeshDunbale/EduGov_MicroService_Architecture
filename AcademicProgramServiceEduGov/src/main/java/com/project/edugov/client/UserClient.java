package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.UserFeignDTO;

/**
 * Feign Client to communicate with the IDENTITY-SERVICE (UserController).
 */
@FeignClient(name = "IDENTITYSERVICEEDUGOV")
public interface UserClient {

	/**
	 * Calls: GET http://IDENTITY-SERVICE/api/users/{id} This matches your
	 * UserController @GetMapping("/{id}")
	 */
	@GetMapping("/api/users/{id}")
	UserFeignDTO getUserById(@PathVariable("id") Long id);
}