package com.project.edugov.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.project.edugov.client.RemoteAuditClient;
import com.project.edugov.dto.AuditLogRequest;

@Service
public class AsyncAuditLogger {

    private final RemoteAuditClient auditClient;

    public AsyncAuditLogger(RemoteAuditClient auditClient) {
        this.auditClient = auditClient;
    }

    // @Async forces this to run on a separate background thread
    @Async
    public void fireAndForgetLog(Long userId, String action, String resource) {
        try {
            AuditLogRequest logReq = new AuditLogRequest();
            logReq.setUserId(userId);
            logReq.setAction(action);
            logReq.setResource(resource);
            
            auditClient.sendLog(logReq);
            System.out.println("Audit log sent successfully in background.");
        } catch (Exception e) {
            // We catch the error so it doesn't crash the main application!
            System.err.println("Failed to send audit log, but keeping system running: " + e.getMessage());
        }
    }
}