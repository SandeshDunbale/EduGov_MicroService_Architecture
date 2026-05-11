package com.project.edugov.controller;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 1. 🟢 Import your actual clients or services here!
import com.project.edugov.clients.facultyClient;
import com.project.edugov.clients.StudentClient;
import com.project.edugov.dto.UserResponseDTO;
import com.project.edugov.model.User;
import com.project.edugov.security.JwtUtil;
import com.project.edugov.service.BlackListedTokenService;
import com.project.edugov.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final ModelMapper modelMapper;
    private final JwtUtil jwtUtil;
    private final BlackListedTokenService blackListService;
    
    // 2. 🟢 Inject the clients/services
    private final facultyClient facultyClient; 
    private final StudentClient studentClient; 

    public AuthController(UserService userService, ModelMapper modelMapper, JwtUtil jwtUtil, 
                          BlackListedTokenService blackListService, 
                          facultyClient facultyClient, StudentClient studentClient) { // 🟢 Add to constructor
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.jwtUtil = jwtUtil;
        this.blackListService = blackListService;
        this.facultyClient = facultyClient;
        this.studentClient = studentClient;
    }

    public record LoginRequest(String email, String password) {}
    public record PasswordResetRequest(String email, String newPassword) {}
    public record JwtAuthResponse(String token, UserResponseDTO user) {}

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginRequest request) {
        User authUser = userService.authenticate(request.email(), request.password());
        
        Long facultyId = null;
        Long studentId = null;
        
        // 3. 🟢 ACTUALLY FETCH THE IDs based on the user's role!
        try {
            if ("FACULTY".equals(authUser.getRole().name())) {
                // Call your service/client to get the faculty details by User ID
            	facultyId = facultyClient.getFacultyByUserId(authUser.getUserId()).getFacultyId();
                
            	// ... inside the try-catch block in your login method ...
            } else if ("STUDENT".equals(authUser.getRole().name())) {
                // 🟢 FIXED: Call getStudentByUserId instead of getStudentById
                studentId = studentClient.getStudentByUserId(authUser.getUserId()).getStudentId();
            }
        } catch (Exception e) {
            System.out.println("❌ FEIGN CLIENT CRASHED DURING LOGIN!");
            e.printStackTrace(); // 👈 THIS IS THE MAGIC LINE
        }
        
        // 4. Generate the token (now it will actually have the real numbers instead of null!)
        String token = jwtUtil.generateToken(authUser.getEmail(), authUser.getRole().name(), authUser.getUserId(), facultyId, studentId);
        
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