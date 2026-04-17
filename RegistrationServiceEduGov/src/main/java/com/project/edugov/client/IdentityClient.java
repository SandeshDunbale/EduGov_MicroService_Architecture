package com.project.edugov.client;

import com.project.edugov.dto.UserCreateRequest;
import com.project.edugov.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// Feign will look for "identity-service" in the Eureka dashboard
@FeignClient(name = "IDENTITYSERVICEEDUGOV") 
public interface IdentityClient {

    @PostMapping("/api/identity/register")
    UserResponseDTO registerUser(@RequestBody UserCreateRequest request);

    @PatchMapping("/api/identity/status/{userId}")
    void updateStatus(@PathVariable("userId") Long userId, @RequestParam("status") String status);
}