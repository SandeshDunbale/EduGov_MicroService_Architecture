package com.project.edugov.service;

import com.project.edugov.client.RemoteUserClient;
import com.project.edugov.dto.RemoteUserDto;
import com.project.edugov.model.Audit;
import com.project.edugov.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired private AuditRepository auditRepository;
    @Autowired private RemoteUserClient userClient;

    public List<Audit> getAllAudits() {
        return auditRepository.findAll();
    }

    public Audit getAuditById(Long id) {
        return auditRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit record not found with ID: " + id));
    }

    @Transactional
    public Audit createAudit(Audit audit, Long creatorId) {
        RemoteUserDto creator = userClient.getUserById(creatorId);
        if (creator == null) {
            throw new RuntimeException("User not found");
        }

        audit.setOfficerId(creatorId);
        if (audit.getDate() == null) audit.setDate(LocalDate.now());
        if (audit.getStatus() == null) audit.setStatus("SCHEDULED");

        Audit savedAudit = auditRepository.save(audit);
        logger.info("User {} action CREATE_AUDIT resource AUDIT_ID: {}", creator.getUserId(), savedAudit.getAuditId());
        return savedAudit;
    }

    @Transactional
    public Audit updateAudit(Long id, Audit auditDetails) {
        Audit audit = getAuditById(id);
        audit.setScope(auditDetails.getScope());
        audit.setDate(auditDetails.getDate());
        audit.setStatus(auditDetails.getStatus());
        audit.setFindings(auditDetails.getFindings());
        return auditRepository.save(audit);
    }

    @Transactional
    public void deleteAudit(Long id) {
        if (!auditRepository.existsById(id)) {
            throw new RuntimeException("Audit not found with ID: " + id);
        }
        auditRepository.deleteById(id);
        logger.warn("Audit record ID: {} has been deleted", id);
    }

    @Transactional
    public Audit reviewAudit(Long auditId, String status, String findings, Long auditorId) {
        logger.info("Initiating audit review for Audit ID: {} by User ID: {}", auditId, auditorId);

        RemoteUserDto auditor = userClient.getUserById(auditorId);
        if (auditor == null) {
            throw new RuntimeException("Auditor not found with ID: " + auditorId);
        }

        if (!"GOVT_AUDITOR".equalsIgnoreCase(auditor.getRole())) {
            throw new RuntimeException("Access Denied: Only Government Auditors can perform this action.");
        }

        Audit audit = getAuditById(auditId);
        audit.setStatus(status);
        audit.setFindings(findings);
        audit.setOfficerId(auditorId);

        Audit updatedAudit = auditRepository.save(audit);

        if ("DENIED".equalsIgnoreCase(status)) {
            logger.info("Audit {} denied; findings: {}", auditId, findings);
        }

        logger.info("User {} action REVIEW_AUDIT resource AUDIT_ID: {} STATUS: {}", auditor.getUserId(), auditId, status);
        return updatedAudit;
    }


}
