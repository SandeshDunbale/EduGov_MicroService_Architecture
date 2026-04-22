package com.project.edugov.controller;

import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;
import com.project.edugov.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(mapToDTO(user)))
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponseDTO>> getUserByRole(@PathVariable Role role) {
        List<UserResponseDTO> users = userService.getUserByRole(role).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<UserResponseDTO>> getUserByStatus(@PathVariable Status status) {
        List<UserResponseDTO> users = userService.getUserByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable Long id, 
            @RequestBody StatusUpdateRequest request) {
        User updatedUser = userService.updateUserStatus(id, request.status());
        return ResponseEntity.ok(mapToDTO(updatedUser));
    }
}
