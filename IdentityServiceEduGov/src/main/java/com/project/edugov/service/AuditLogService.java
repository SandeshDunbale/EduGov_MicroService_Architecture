package com.project.edugov.service;

import com.project.edugov.model.AuditLog;
import com.project.edugov.model.User;

import java.util.List;

public interface AuditLogService {

    /**
     * Logs an action for the currently authenticated user.
     *
     * @param action   The action performed (e.g., "APPROVE_STUDENT")
     * @param resource The resource affected (e.g., "Student ID: 123")
     */
    void logAction(String action, String resource);

    /**
     * Logs an action for a specific user, typically used when the 
     * security context is not yet populated (e.g., during Login).
     *
     * @param user     The user performing the action
     * @param action   The action performed
     * @param resource The resource affected
     */
    void logActionForUser(User user, String action, String resource);

    /**
     * Fetches all audit logs.
     *
     * @return A list of all audit logs
     */
    List<AuditLog> getAllLogs();
    
    
}