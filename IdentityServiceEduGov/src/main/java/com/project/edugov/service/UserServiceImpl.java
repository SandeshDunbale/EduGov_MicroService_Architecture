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
    private final NotificationClient notificationClient; // 1. Add the client

    // 2. Inject it via the constructor
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, NotificationClient notificationClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationClient = notificationClient;
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
        return user;
    }

    @Override
    public void updatePassword(String email, String newRawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        
        // Microservice decoupled logging (No direct NotificationService call)
        logger.info("ACTION: Password updated for user. (Ready for Kafka event)");
        try {
            notificationClient.sendNotification(
                    user.getUserId(), // userId
                    user.getUserId(), // entityId (we can just use userId here)
                    "Your password has been successfully updated. If you did not make this change, please contact support.", 
                    "SECURITY_ALERT", // Category
                    user.getEmail()   // Email
            );
        } catch (Exception e) {
            logger.error("Failed to send password update notification: " + e.getMessage());
            // We catch the exception so that if the Notification Service is down,
            // the user's password update still succeeds!
        }
    }

    // Keep your other methods here exactly as they were: 
    // getUserByRole, getUserByStatus, updateUserStatus, getUserById, recoverEmailByPhone
    
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
        return userRepository.save(user);
    }
    @Override
    public User registerUser(IdentityController.UserCreateRequest request) {
        // Check if the user already exists to avoid SQL errors
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email " + request.email() + " is already registered!");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setDob(request.dob());
        
        // Crucial: Hash the password using the existing passwordEncoder bean
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        
        // Set default values so they can actually use the account
        user.setRole(Role.valueOf(request.role().toUpperCase()));
        user.setStatus(Status.ACTIVE); // Or Status.PENDING if you want admin approval first

        return userRepository.save(user);
    }
    
    
    @Override
    public void deleteUser(Long userId) {
      //  log.info("Identity Service: Deleting user record for ID: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        
        userRepository.deleteById(userId);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}