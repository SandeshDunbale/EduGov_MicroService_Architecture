package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.AuditLog;
import com.project.edugov.model.User;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Custom queries for the Government Auditor to filter logs
//    List<AuditLog> findByUser_UserId(Long userId);
//    List<AuditLog> findByAction(String action);
//    List<AuditLog> findByResource(String resource);
    
    // Fetches all logs ordered by the newest first
    List<AuditLog> findAllByOrderByTimestampDesc(); 
    
    List<AuditLog> findByUser(User user);

    // Find logs for a specific resource (e.g., "AUDIT_MODULE")
    List<AuditLog> findByResource(String resource);
}