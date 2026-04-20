package com.project.edugov.controller;

import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.model.User;
import com.project.edugov.security.JwtUtil;
import com.project.edugov.service.BlackListedTokenService;
import com.project.edugov.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final ModelMapper modelMapper;
    private final JwtUtil jwtUtil;
    private final BlackListedTokenService blackListService;

    public AuthController(UserService userService, ModelMapper modelMapper, JwtUtil jwtUtil, BlackListedTokenService blackListService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.jwtUtil = jwtUtil;
        this.blackListService = blackListService;
    }

    public record LoginRequest(String email, String password) {}
    public record PasswordResetRequest(String email, String newPassword) {}
    public record JwtAuthResponse(String token, UserResponseDTO user) {}

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginRequest request) {
        User authUser = userService.authenticate(request.email(), request.password());
        String token = jwtUtil.generateToken(authUser.getEmail(), authUser.getRole().name());
        
        UserResponseDTO userDTO = modelMapper.map(authUser, UserResponseDTO.class);
        return ResponseEntity.ok(new JwtAuthResponse(token, userDTO));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            blackListService.addToBlacklist(jwt);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok("Logout successful. Token invalidated.");
        }
        return ResponseEntity.badRequest().body("No valid token provided.");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest request) {
        userService.updatePassword(request.email(), request.newPassword());
        return ResponseEntity.ok("Password updated successfully.");
    }
}