package com.project.edugov.controller;

import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;
import com.project.edugov.repository.UserRepository;
import com.project.edugov.service.UserService;

import java.time.LocalDate;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final UserService userService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    /**
     * Data Transfer Object (Record) for Registration
     */
    public record UserCreateRequest(String name, String email, String password, String phone, String role,LocalDate dob) {}

    public IdentityController(UserService userService, ModelMapper modelMapper,UserRepository userRepository) {
        this.userService = userService;
        this.modelMapper = modelMapper;
		this.userRepository = userRepository;
    }

    /**
     * Endpoint called by Registration Service to create login credentials
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@RequestBody UserCreateRequest request) {
        User savedUser = userService.registerUser(request);
        return ResponseEntity.ok(modelMapper.map(savedUser, UserResponseDTO.class));
    }
    
    
    
    
    
    
    
    
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        // Call service instead of repo directly
        userService.deleteUser(userId); 
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint called by Registration Service to Approve/Decline/Deactivate users.
     * Note: I used PutMapping here for maximum compatibility with Feign.
     */
    /**
     * Endpoint changed to PatchMapping to match the Feign Client
     */
    @PatchMapping("/status/{userId}") // Change this from @PutMapping
    public ResponseEntity<UserResponseDTO> updateUserStatus(
    		@PathVariable("userId") Long userId,
            @RequestParam("status") Status status) {
        
        User updatedUser = userService.updateUserStatus(userId, status);
        return ResponseEntity.ok(modelMapper.map(updatedUser, UserResponseDTO.class));
    }
//    
    
    
//    
//    @Override
//    @Transactional
//    public User updateUserStatus(Long userId, Status status) {
//        // 1. Fetch the full user record from DB
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // 2. Update the status
//        user.setStatus(status);
//
//        // 3. Save and return the PERSISTED object (which has the email)
//        return userRepository.save(user); 
//    }
    
}