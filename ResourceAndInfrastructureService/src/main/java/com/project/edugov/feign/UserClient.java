package com.project.edugov.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.UserDTO;

@FeignClient(name = "IDENTITYSERVICEEDUGOV")
public interface UserClient {

    // FIX: Pointed to the correct, permitted path exposed by UserController
    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(@PathVariable("userId") Long userId);

    // FIX: Update this path as well to match your actual controller routing
    @GetMapping("/api/users/{userId}/active") 
    Boolean isUserActive(@PathVariable("userId") Long userId);
}