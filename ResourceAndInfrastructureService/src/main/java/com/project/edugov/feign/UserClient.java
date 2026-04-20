package com.project.edugov.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.UserDTO;

@FeignClient(
        name = "IDENTITYSERVICEEDUGOV",   // ⚠️ lowercase recommended
        path = "/api/users"
)
public interface UserClient {

    @GetMapping("/{userId}")
    UserDTO getUserById(@PathVariable Long userId);
    
    @GetMapping("/{userId}/active")
    Boolean isUserActive(@PathVariable("userId") Long userId);
}