package com.project.edugov.service;

import com.project.edugov.feign.RemoteAuditClient;
import com.project.edugov.dto.AuditLogRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AsyncAuditLogger {

    private final RemoteAuditClient auditClient;

    public AsyncAuditLogger(RemoteAuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @Async
    public void fireAndForgetLog(Long userId, String action, String resource) {
        try {
            AuditLogRequest logReq = new AuditLogRequest();
            logReq.setUserId(userId);
            logReq.setAction(action);
            logReq.setResource(resource);
            
            auditClient.sendLog(logReq);
            log.info("Academic audit log sent successfully in background.");
        } catch (Exception e) {
            log.error("Failed to send audit log, but keeping system running: {}", e.getMessage());
        }
    }
}