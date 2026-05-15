package com.project.edugov.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.edugov.dto.AuditLogRequest;
import com.project.edugov.model.AuditLog;
import com.project.edugov.model.User;
import com.project.edugov.repository.AuditLogRepository;
import com.project.edugov.repository.UserRepository;

@Service
public class AuditLogServiceImpl implements AuditLogService { // <-- FIXED: Added implements

    private final AuditLogRepository auditRepo;
    private final UserRepository userRepo;

    public AuditLogServiceImpl(AuditLogRepository auditRepo, UserRepository userRepo) {
        this.auditRepo = auditRepo;
        this.userRepo = userRepo;
    }

    // 1. Method to handle incoming logs from OTHER microservices
    public void logActionFromMicroservice(AuditLogRequest request) {
        User user = userRepo.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found for audit log: " + request.getUserId()));

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(request.getAction());
        log.setResource(request.getResource());
        log.setTimestamp(LocalDateTime.now());
        
        auditRepo.save(log);
    }

    // ========================================================================
    // METHODS REQUIRED BY THE INTERFACE (For local use inside Identity Service)
    // ========================================================================

    @Override
    public void logActionForUser(User user, String action, String resource) {
        AuditLog log = new AuditLog();
        log.setUser(user); // We already have the User object, no need to look it up!
        log.setAction(action);
        log.setResource(resource);
        log.setTimestamp(LocalDateTime.now());
        
        auditRepo.save(log);
    }

    @Override
    public void logAction(String action, String resource) {
        // If you need to log an action without passing the user directly, 
        // you would extract the user ID from the SecurityContextHolder here.
        // For now, your UserServiceImpl uses logActionForUser, so this can stay empty or throw an error.
        throw new UnsupportedOperationException("Please use logActionForUser() instead.");
    }

    @Override
    public List<AuditLog> getAllLogs() {
        // Assuming your repository has a method to fetch all logs, preferably ordered by newest first
        return auditRepo.findAll(); 
    }
}