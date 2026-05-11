package com.project.edugov.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.edugov.client.NotificationClient;
import com.project.edugov.client.RemoteProgramClient;
import com.project.edugov.client.RemoteResearchAndGrantServiceClient;
import com.project.edugov.client.RemoteStudentClient;
import com.project.edugov.client.RemoteUserClient;
import com.project.edugov.dto.ComplianceRecordDTO;
import com.project.edugov.dto.RemoteProgramDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantApplicationDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantDto;
import com.project.edugov.dto.RemoteStudentDto;
import com.project.edugov.dto.RemoteUserDto;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.ComplianceRecord;
import com.project.edugov.repository.ComplianceRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class ComplianceServiceImpl implements ComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceServiceImpl.class);

    @Autowired private ComplianceRepository complianceRepo;
    @Autowired private RemoteUserClient userClient;
    @Autowired private RemoteResearchAndGrantServiceClient researchAndGrantClient;
    @Autowired private RemoteProgramClient programClient;
    @Autowired private RemoteStudentClient studentClient;
    @Autowired private NotificationClient notificationClient;

    @Override
    @CircuitBreaker(name = "complianceApi", fallbackMethod = "fallbackGenerate")
    public void generateCompliance(Long officerId) {
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) throw new ResourceNotFoundException("Officer not found: " + officerId);

        List<GrantApplicationDto> approvedApplications = researchAndGrantClient.getGrantApplicationsByStatus(Arrays.asList("APPROVED", "COMPLETED"));
        if (approvedApplications != null) {
            approvedApplications.forEach(app -> {
                if (app != null && app.getProject() != null) {
                    GrantDto grant = researchAndGrantClient.getGrantByProjectId(app.getProject().getProjectId());
                    if (grant == null) saveInternalCompliance(app.getProject().getProjectId(), "PROJECT", "Missing Grant", officer);
                }
            });
        }

        List<RemoteProgramDto> inactivePrograms = programClient.getByStatus("INACTIVE");
        List<RemoteStudentDto> allStudents = studentClient.getAllStudents();
        if (inactivePrograms != null && allStudents != null) {
            inactivePrograms.forEach(p -> {
                if (allStudents.stream().anyMatch(s -> p.getProgramID().equals(s.getProgramId()))) {
                    saveInternalCompliance(p.getProgramID(), "PROGRAM", "Students in inactive program", officer);
                }
            });
        }
    }

    // RESTORED: Implementation for getAllCompliance
    @Override
    public List<ComplianceRecordDTO> getAllCompliance() {
        return complianceRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // RESTORED: Implementation for getComplianceById
    @Override
    @CircuitBreaker(name = "complianceApi", fallbackMethod = "fallbackGetById")
    public ComplianceRecordDTO getComplianceById(Long id) {
        ComplianceRecord record = complianceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record " + id + " not found"));
        return convertToDTO(record);
    }

    // RESTORED: Implementation for deleteCompliance
    @Override
    public void deleteCompliance(Long id) {
        if (!complianceRepo.existsById(id)) throw new ResourceNotFoundException("Not found");
        complianceRepo.deleteById(id);
    }

    @Override
    @CircuitBreaker(name = "complianceApi", fallbackMethod = "fallbackCreateManual")
    public ComplianceRecordDTO createManual(ComplianceRecord record, Long officerId) {
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) throw new ResourceNotFoundException("User not found");
        record.setOfficerId(officerId);
        record.setDate(LocalDate.now());
        return convertToDTO(complianceRepo.save(record));
    }

    @Override
    public ComplianceRecordDTO updateCompliance(Long id, ComplianceRecord details) {
        ComplianceRecord existing = complianceRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        existing.setNotes(details.getNotes());
        existing.setResult(details.getResult());
        ComplianceRecord saved = complianceRepo.save(existing);

        if ("REPORT_GENERATED".equalsIgnoreCase(details.getResult()) || "COMPLETED".equalsIgnoreCase(details.getResult())) {
            String msg = "Compliance Report Generated for Entity: " + existing.getEntityType();
            notificationClient.sendNotification(existing.getOfficerId(), id, msg, "COMPLIANCE_REPORT", null);
            userClient.getUsersByRole("UNIV_ADMIN").forEach(admin -> 
                notificationClient.sendNotification(admin.getUserId(), id, msg, "COMPLIANCE_REPORT", admin.getEmail())
            );
        }

        return convertToDTO(saved);
    }

    private void saveInternalCompliance(Long id, String type, String notes, RemoteUserDto officer) {
        if (!complianceRepo.existsByEntityIdAndEntityType(id, type)) {
            ComplianceRecord record = new ComplianceRecord();
            record.setEntityId(id); record.setEntityType(type);
            record.setNotes(notes); record.setDate(LocalDate.now());
            record.setOfficerId(officer.getUserId()); record.setResult("UNDER_REVIEW");
            complianceRepo.save(record);

            if ("PROJECT".equalsIgnoreCase(type) || "GRANT".equalsIgnoreCase(type)) {
                userClient.getUsersByRole("UNIV_ADMIN").forEach(admin -> 
                    notificationClient.sendNotification(admin.getUserId(), id, 
                    "Compliance Error in " + type + ": " + notes, "COMPLIANCE_ERROR", admin.getEmail())
                );
            }
        }
    }

    // --- FALLBACKS & HELPERS ---

    public void fallbackGenerate(Long officerId, Throwable t) {
        logger.error("Scan failed for Officer {}: {}", officerId, t.getMessage());
    }

    public ComplianceRecordDTO fallbackGetById(Long id, Throwable t) {
        logger.error("Fallback for ID {}: {}", id, t.getMessage());
        ComplianceRecordDTO dto = new ComplianceRecordDTO();
        dto.setComplianceId(id);
        dto.setResult("SERVICE_UNAVAILABLE");
        return dto;
    }

    public ComplianceRecordDTO fallbackCreateManual(ComplianceRecord record, Long officerId, Throwable t) {
        ComplianceRecordDTO dto = new ComplianceRecordDTO();
        dto.setNotes("Service unavailable. Manual creation failed.");
        return dto;
    }

    private ComplianceRecordDTO convertToDTO(ComplianceRecord record) {
        ComplianceRecordDTO dto = new ComplianceRecordDTO();
        dto.setComplianceId(record.getComplianceId());
        dto.setEntityId(record.getEntityId());
        dto.setEntityType(record.getEntityType());
        dto.setResult(record.getResult());
        dto.setDate(record.getDate());
        dto.setNotes(record.getNotes());
        dto.setOfficerId(record.getOfficerId());
        try {
            RemoteUserDto user = userClient.getUserById(record.getOfficerId());
            if (user != null) dto.setOfficerName(user.getName());
        } catch (Exception ignored) {
            dto.setOfficerName("Service Unavailable");
        }
        return dto;
    }
}