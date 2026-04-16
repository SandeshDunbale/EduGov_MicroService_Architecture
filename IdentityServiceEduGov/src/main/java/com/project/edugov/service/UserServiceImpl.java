package com.project.edugov.service;

import com.project.edugov.exception.AccountNotActiveException;
import com.project.edugov.exception.InvalidCredentialsException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;
import com.project.edugov.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}