package com.project.edugov.service;

import com.project.edugov.client.RemoteUserClient;
import com.project.edugov.dto.RemoteUserDto;
import com.project.edugov.exception.AccessDeniedException;
import com.project.edugov.exception.ResourceNotFoundException;
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
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    @Autowired 
    private AuditRepository auditRepository;
    
    @Autowired 
    private RemoteUserClient userClient;

    @Override
    public List<Audit> getAllAudits() {
        return auditRepository.findAll();
    }

    @Override
    public Audit getAuditById(Long id) {
        return auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit record not found with ID: " + id));
    }

    @Override
    @Transactional
    public Audit createAudit(Audit audit, Long creatorId) {
        RemoteUserDto creator = userClient.getUserById(creatorId);
        if (creator == null) {
            throw new ResourceNotFoundException("User not found with ID: " + creatorId);
        }

        audit.setOfficerId(creatorId);
        if (audit.getDate() == null) audit.setDate(LocalDate.now());
        if (audit.getStatus() == null) audit.setStatus("SCHEDULED");

        Audit savedAudit = auditRepository.save(audit);
        logger.info("Audit created by User {}: ID {}", creatorId, savedAudit.getAuditId());
        return savedAudit;
    }

    @Override
    @Transactional
    public Audit updateAudit(Long id, Audit auditDetails) {
        Audit audit = getAuditById(id);
        audit.setScope(auditDetails.getScope());
        audit.setDate(auditDetails.getDate());
        audit.setStatus(auditDetails.getStatus());
        audit.setFindings(auditDetails.getFindings());
        return auditRepository.save(audit);
    }

    @Override
    @Transactional
    public void deleteAudit(Long id) {
        if (!auditRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete: Audit ID " + id + " does not exist.");
        }
        auditRepository.deleteById(id);
        logger.warn("Audit record ID: {} deleted", id);
    }

    @Override
    @Transactional
    public Audit reviewAudit(Long auditId, String status, String findings, Long auditorId) {
        RemoteUserDto auditor = userClient.getUserById(auditorId);
        if (auditor == null) {
            throw new ResourceNotFoundException("Auditor not found with ID: " + auditorId);
        }

        if (!"GOVT_AUDITOR".equalsIgnoreCase(auditor.getRole())) {
            throw new AccessDeniedException("Permission Denied: Only Government Auditors can review audits.");
        }

        Audit audit = getAuditById(auditId);
        audit.setStatus(status);
        audit.setFindings(findings);
        audit.setOfficerId(auditorId);

        logger.info("Audit ID {} {} by Auditor {}", auditId, status, auditorId);
        return auditRepository.save(audit);
    }
}