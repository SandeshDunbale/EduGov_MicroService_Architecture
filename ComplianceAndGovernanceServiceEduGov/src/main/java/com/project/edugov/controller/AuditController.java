package com.project.edugov.controller;

import com.project.edugov.dto.AuditReviewDTO;
import com.project.edugov.model.Audit;
import com.project.edugov.service.AuditServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
@CrossOrigin(origins = "*")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    @Autowired
    private AuditServiceImpl auditService;

    @PostMapping("/create")
    public ResponseEntity<Audit> createAudit(@RequestBody Audit audit, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(auditService.createAudit(audit, userId));
    }

   
    @GetMapping("/get")
    public ResponseEntity<List<Audit>> getAllAudits() {
        return ResponseEntity.ok(auditService.getAllAudits());
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<Audit> getAudit(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getAuditById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Audit> updateAudit(@PathVariable Long id, @RequestBody Audit audit) {
        return ResponseEntity.ok(auditService.updateAudit(id, audit));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteAudit(@PathVariable Long id) {
        auditService.deleteAudit(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/review/{id}")
    public ResponseEntity<Audit> reviewAudit(
            @PathVariable Long id,
            @RequestBody AuditReviewDTO reviewDto,
            @RequestHeader("X-User-Id") Long auditorId) {

        Audit updatedAudit = auditService.reviewAudit(id, reviewDto.getStatus(), reviewDto.getFindings(), auditorId);
        return ResponseEntity.ok(updatedAudit);
    }
}