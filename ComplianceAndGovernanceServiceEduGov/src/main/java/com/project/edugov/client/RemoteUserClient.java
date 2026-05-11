package com.project.edugov.client;

import com.project.edugov.dto.RemoteUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List; // Changed to List for better compatibility with Stream logic

@FeignClient(name = "IdentityServiceEduGov", path = "/api/users")
public interface RemoteUserClient {

    @GetMapping("/{id}")
    RemoteUserDto getUserById(@PathVariable("id") Long id);

    // Added GetMapping and PathVariable
    @GetMapping("/role/{role}")
    List<RemoteUserDto> getUsersByRole(@PathVariable("role") String role);
}