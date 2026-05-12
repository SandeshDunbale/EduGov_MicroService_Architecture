package com.project.edugov.controller;

import com.project.edugov.dto.AuditLogRequest;
import com.project.edugov.service.AuditLogServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class InternalAuditController {

    private final AuditLogServiceImpl auditService;

    public InternalAuditController(AuditLogServiceImpl auditService) {
        this.auditService = auditService;
    }

    // This is an INTERNAL route. Do not expose this in the API Gateway!
    @PostMapping("/internal/log")
    public void receiveLogFromMicroservice(@RequestBody AuditLogRequest request) {
        auditService.logActionFromMicroservice(request);
    }
}