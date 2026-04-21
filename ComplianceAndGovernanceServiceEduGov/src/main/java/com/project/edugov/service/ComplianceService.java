package com.project.edugov.service;

import com.project.edugov.client.RemoteResearchAndGrantServiceClient;
import com.project.edugov.client.RemoteProgramClient;
import com.project.edugov.client.RemoteStudentClient;
import com.project.edugov.client.RemoteUserClient;
import com.project.edugov.dto.ComplianceRecordDTO;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantApplicationDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantDto;
import com.project.edugov.dto.RemoteProgramDto;
import com.project.edugov.dto.RemoteStudentDto;
import com.project.edugov.dto.RemoteUserDto;
import com.project.edugov.model.ComplianceRecord;
import com.project.edugov.repository.ComplianceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
// Transaction management is not currently needed in this service.

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplianceService {

    // logger intentionally omitted until logging is required

    @Autowired
    private ComplianceRepository complianceRepo;

    @Autowired
    private RemoteUserClient userClient;

    @Autowired
    private RemoteResearchAndGrantServiceClient researchAndGrantClient;

    @Autowired
    private RemoteProgramClient programClient;

    @Autowired
    private RemoteStudentClient studentClient;

    public void generateCompliance(Long officerId) {
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) {
            throw new RuntimeException("Officer not found");
        }

        List<GrantApplicationDto> approvedApplications = researchAndGrantClient.getGrantApplicationsByStatus(Arrays.asList("APPROVED", "COMPLETED"));
        if (approvedApplications != null) {
            approvedApplications.forEach(app -> {
                if (app != null && app.getProject() != null) {
                    GrantDto grant = researchAndGrantClient.getGrantByProjectId(app.getProject().getProjectId());
                    if (grant == null) {
                        saveCompliance(app.getProject().getProjectId(), "PROJECT", "Missing Grant for " + app.getStatus() + " app.", officer);
                    }
                }
            });
        }

        List<RemoteProgramDto> inactivePrograms = programClient.getByStatus("INACTIVE");
        List<RemoteStudentDto> allStudents = studentClient.getAllStudents();
        if (inactivePrograms != null && allStudents != null) {
            inactivePrograms.forEach(program -> {
                if (program != null && program.getProgramID() != null) {
                    boolean hasStudents = allStudents.stream()
                        .anyMatch(student -> program.getProgramID().equals(student.getProgramId()));
                    if (hasStudents) {
                        saveCompliance(program.getProgramID(), "PROGRAM", "Students in inactive program.", officer);
                    }
                }
            });
        }

        List<GrantDto> allGrants = researchAndGrantClient.getAllGrants();
        if (allGrants != null) {
            allGrants.forEach(grant -> {
                if (grant != null && grant.getProject() != null && grant.getAmount() != null) {
                    GrantApplicationDto app = researchAndGrantClient.getGrantApplicationByProjectId(grant.getProject().getProjectId());
                    if (app != null && app.getRequestedAmount() != null && grant.getAmount().compareTo(app.getRequestedAmount()) > 0) {
                        saveCompliance(grant.getProject().getProjectId(), "PROJECT", "Over-funding detected.", officer);
                    }
                }
            });
        }
    }

    private void saveCompliance(Long id, String type, String notes, RemoteUserDto officer) {
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

    public ComplianceRecordDTO createManual(ComplianceRecord record, Long officerId) {
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) {
            throw new RuntimeException("User not found");
        }
        record.setOfficerId(officerId);
        record.setDate(LocalDate.now());
        return convertToDTO(complianceRepo.save(record));
    }

    public List<ComplianceRecordDTO> getAllCompliance() {
        return complianceRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public ComplianceRecordDTO updateCompliance(Long id, ComplianceRecord details) {
        ComplianceRecord existing = complianceRepo.findById(id).orElseThrow(() -> new RuntimeException("Not Found"));
        existing.setNotes(details.getNotes());
        existing.setResult(details.getResult());
        return convertToDTO(complianceRepo.save(existing));
    }

    public void deleteCompliance(Long id) {
        complianceRepo.deleteById(id);
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
            RemoteUserDto officer = userClient.getUserById(record.getOfficerId());
            if (officer != null) {
                dto.setOfficerName(officer.getName());
            }
        } catch (Exception ignored) {
            // remote lookup may fail; leave officerName null
        }
        return dto;
    }
}
