package com.project.edugov.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;
import com.project.edugov.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    public record StatusUpdateRequest(Status status) {}

    private UserResponseDTO mapToDTO(User user) {
        return modelMapper.map(user, UserResponseDTO.class);
    }

    @GetMapping("/recoverEmail")
    public ResponseEntity<String> recoverEmail(@RequestParam String phone) {
        String email = userService.recoverEmailByPhone(phone);
        return ResponseEntity.ok(email);
    }
    
 // Use hasAnyAuthority to check for the exact string match without the ROLE_ prefix!
 // Bulletproof authority check for all roles, with and without prefixes
 // Simplified to use the roles that actually exist in your Role enum
    @PreAuthorize("hasAnyAuthority('UNIV_ADMIN', 'ROLE_UNIV_ADMIN', 'FACULTY', 'ROLE_FACULTY', 'STUDENT', 'ROLE_STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(mapToDTO(user)))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }
    
    @PreAuthorize("hasAnyRole('UNIV_ADMIN', 'PROG_MANAGER')")
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponseDTO>> getUserByRole(@PathVariable Role role) {
        List<UserResponseDTO> users = userService.getUserByRole(role).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
    
    @PreAuthorize("hasRole('UNIV_ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<UserResponseDTO>> getUserByStatus(@PathVariable Status status) {
        List<UserResponseDTO> users = userService.getUserByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
    
    @PreAuthorize("hasAnyRole('UNIV_ADMIN')")
    @PatchMapping("/status/{id}")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable Long id, 
            @RequestBody StatusUpdateRequest request) {
        User updatedUser = userService.updateUserStatus(id, request.status());
        return ResponseEntity.ok(mapToDTO(updatedUser));
    }
}
