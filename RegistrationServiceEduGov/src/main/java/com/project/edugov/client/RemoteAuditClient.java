package com.project.edugov.client;

import com.project.edugov.dto.AuditLogRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Name MUST match the Identity Service name in Eureka
@FeignClient(name = "IDENTITYSERVICEEDUGOV", contextId = "auditClient", path = "/api/audit/internal")
public interface RemoteAuditClient {

    @PostMapping("/log")
    void sendLog(@RequestBody AuditLogRequest request);
}