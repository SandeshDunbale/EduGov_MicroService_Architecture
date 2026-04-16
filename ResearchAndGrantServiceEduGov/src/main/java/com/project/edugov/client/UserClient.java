package com.project.edugov.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.UserExternalDTO;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserClient {

    // Used to verify if the user approving the grant is actually a Program Manager
    @GetMapping("/api/users/{id}")
    UserExternalDTO getUserById(@PathVariable("id") Long userId);

    // Used by your GrantServiceImpl to fetch all PMs so it can send them notifications!
    @GetMapping("/api/users/role/{role}")
    List<UserExternalDTO> getUsersByRole(@PathVariable("role") String role);
    
}