package com.project.edugov.client;

import com.project.edugov.dto.RemoteUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ✅ Added the 'path' attribute. 
// Change "/api/users" to exactly match the @RequestMapping of your Identity Service controller.
@FeignClient(name = "IDENTITYSERVICEEDUGOV", path = "/api/users")
public interface RemoteUserClient {

    @GetMapping("/{id}")
    RemoteUserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/role/{role}")
    List<RemoteUserDto> getUsersByRole(@PathVariable("role") String role);
}