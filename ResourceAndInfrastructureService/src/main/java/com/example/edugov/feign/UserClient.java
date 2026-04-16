package com.example.edugov.feign;

import com.example.edugov.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "IdentityServiceEduGov",
        path = "/api/users"
)
public interface UserClient {

    @GetMapping("/{userId}")
    UserDTO getUserById(@PathVariable Long userId);
}