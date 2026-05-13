package com.project.edugov.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.edugov.clients.NotificationClient;
import com.project.edugov.controller.IdentityController;
import com.project.edugov.exception.AccountNotActiveException;
import com.project.edugov.exception.InvalidCredentialsException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;
import com.project.edugov.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;
    
    // 1. INJECT YOUR EXISTING LOCAL AUDIT LOG SERVICE
    private final AuditLogService auditLogService; 

    // 2. Add it to the constructor
    public UserServiceImpl(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder, 
            NotificationClient notificationClient,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationClient = notificationClient;
        this.auditLogService = auditLogService;
    }

    @Override
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Email or Password"));

        if (user.getStatus() != Status.ACTIVE && user.getStatus() != Status.APPROVE) {
            throw new AccountNotActiveException("Account is Currently " + user.getStatus());
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        // 3. LOG LOGIN EVENT
        auditLogService.logActionForUser(user, "LOGIN", "User Authenticated Successfully");
        
        return user;
    }

    @Override
    public void updatePassword(String email, String phone, java.time.LocalDate dob, String newRawPassword) {
        // 1. Fetch User by Email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with the provided email."));
        
        // 2. Identity Verification: Match Phone Number
        if (user.getPhone() == null || !user.getPhone().equals(phone)) {
            // We use a generic error message so hackers don't know exactly WHICH field failed
            throw new InvalidCredentialsException("Verification failed: The provided details do not match our records.");
        }

        // 3. Identity Verification: Match Date of Birth
        if (user.getDob() == null || !user.getDob().equals(dob)) {
            throw new InvalidCredentialsException("Verification failed: The provided details do not match our records.");
        }

        // 4. Password Strength Validation
        if (!isValidPassword(newRawPassword)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, a number, and a special character.");
        }

        // 5. Prevent Reusing the Old Password
        if (passwordEncoder.matches(newRawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Your new password cannot be the same as your current password.");
        }
        
        // 6. Save New Password
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        
        // 7. LOG PASSWORD UPDATE
        auditLogService.logActionForUser(user, "UPDATE_PASSWORD", "User successfully reset their password");
        
        logger.info("ACTION: Password updated for user. Sending security alert...");
        try {
            notificationClient.sendNotification(
                    user.getUserId(), 
                    user.getUserId(), 
                    "Your password has been successfully updated. If you did not make this change, please contact support immediately.", 
                    "SECURITY_ALERT", 
                    user.getEmail()   
            );
        } catch (Exception e) {
            logger.error("Failed to send password update notification: " + e.getMessage());
        }
    }

    // --- Add this Helper Method anywhere in your UserServiceImpl ---
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        // Regex: Min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special character
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }

    @Override
    public List<User> getUserByRole(Role role) { return userRepository.findByRole(role); }
    @Override
    public List<User> getUserByStatus(Status status) { return userRepository.findByStatus(status); }
    @Override
    public Optional<User> getUserById(Long userId) { return userRepository.findById(userId); }
    
    @Override
    public String recoverEmailByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("No account found.")).getEmail();
    }
    
    @Override
    public User updateUserStatus(Long userId, Status newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(newStatus);
        User savedUser = userRepository.save(user);
        
        // 5. LOG STATUS UPDATE
        auditLogService.logActionForUser(savedUser, "UPDATE_STATUS", "Status changed to " + newStatus);
        
        return savedUser;
    }
    
    @Override
    public User registerUser(IdentityController.UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email " + request.email() + " is already registered!");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setDob(request.dob());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.valueOf(request.role().toUpperCase()));
        user.setStatus(Status.ACTIVE); 

        User savedUser = userRepository.save(user);
        
        // 6. LOG USER REGISTRATION
        auditLogService.logActionForUser(savedUser, "REGISTER_USER", "Role assigned: " + request.role());
        return userRepository.saveAndFlush(user);

    }
    
    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // 7. LOG BEFORE DELETION
        auditLogService.logActionForUser(user, "DELETE_USER", "User account removed");
        
        userRepository.deleteById(userId);
    }
}