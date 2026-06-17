package com.project.edugov.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

@Service
public class ComplianceServiceImpl implements ComplianceService {

    @Autowired private ComplianceRepository complianceRepo;
    @Autowired private RemoteUserClient userClient;
    @Autowired private RemoteResearchAndGrantServiceClient researchAndGrantClient;
    @Autowired private RemoteProgramClient programClient;
    @Autowired private RemoteStudentClient studentClient;

    @Override
    public void generateCompliance(Long officerId) {
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) {
            throw new ResourceNotFoundException("Compliance Officer not found with ID: " + officerId);
        }

        // 1. Missing Grant Check
        List<GrantApplicationDto> approvedApplications = researchAndGrantClient.getGrantApplicationsByStatus(Arrays.asList("APPROVED", "COMPLETED"));
        if (approvedApplications != null) {
            approvedApplications.forEach(app -> {
                if (app != null && app.getProject() != null) {
                    GrantDto grant = researchAndGrantClient.getGrantByProjectId(app.getProject().getProjectId());
                    if (grant == null) {
                        saveInternalCompliance(app.getProject().getProjectId(), "PROJECT", "Missing Grant for " + app.getStatus() + " app.", officer);
                    }
                }
            });
        }

        // 2. Inactive Program Check
        List<RemoteProgramDto> inactivePrograms = programClient.getByStatus("INACTIVE");
        List<RemoteStudentDto> allStudents = studentClient.getAllStudents();
        if (inactivePrograms != null && allStudents != null) {
            inactivePrograms.forEach(program -> {
                boolean hasStudents = allStudents.stream()
                        .anyMatch(s -> program.getProgramID().equals(s.getProgramId()));
                if (hasStudents) {
                    saveInternalCompliance(program.getProgramID(), "PROGRAM", "Students in inactive program.", officer);
                }
            });
        }

        // 3. Over-funding Check
        List<GrantDto> allGrants = researchAndGrantClient.getAllGrants();
        if (allGrants != null) {
            allGrants.forEach(grant -> {
                // FIXED: Call getProjectId() directly on the grant
                GrantApplicationDto app = researchAndGrantClient.getGrantApplicationByProjectId(grant.getProjectId());
                
                // FIXED: Added null checks for safety and converted Double to BigDecimal for comparison
                if (app != null && grant.getAmount() != null && app.getRequestedAmount() != null) {
                    BigDecimal grantAmount = BigDecimal.valueOf(grant.getAmount());
                    
                    if (grantAmount.compareTo(app.getRequestedAmount()) > 0) {
                        // FIXED: Call getProjectId() directly here as well
                        saveInternalCompliance(grant.getProjectId(), "PROJECT", "Over-funding detected.", officer);
                    }
                }
            });
        }
    }

    @Override
    public ComplianceRecordDTO createManual(ComplianceRecord record, Long officerId) {
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) throw new ResourceNotFoundException("User not found");
        
        record.setOfficerId(officerId);
        record.setDate(LocalDate.now());
        return convertToDTO(complianceRepo.save(record));
    }

    @Override
    public List<ComplianceRecordDTO> getAllCompliance() {
        return complianceRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public ComplianceRecordDTO updateCompliance(Long id, ComplianceRecord details) {
        ComplianceRecord existing = complianceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance record " + id + " not found"));
        existing.setNotes(details.getNotes());
        existing.setResult(details.getResult());
        return convertToDTO(complianceRepo.save(existing));
    }

    @Override
    public void deleteCompliance(Long id) {
        if (!complianceRepo.existsById(id)) throw new ResourceNotFoundException("Record " + id + " not found");
        complianceRepo.deleteById(id);
    }

    // --- Internal Helpers ---

    private void saveInternalCompliance(Long id, String type, String notes, RemoteUserDto officer) {
        if (!complianceRepo.existsByEntityIdAndEntityType(id, type)) {
            ComplianceRecord record = new ComplianceRecord();
            record.setEntityId(id);
            record.setEntityType(type);
            record.setNotes(notes);
            record.setDate(LocalDate.now());
            record.setOfficerId(officer.getUserId());
            record.setResult("UNDER_REVIEW");
            complianceRepo.save(record);
        }
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
        } catch (Exception ignored) {}
        return dto;
    }
}