package com.project.edugov.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.*;
import com.project.edugov.dto.*;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantApplicationDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantDto;
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
//    @Autowired private RemoteStudentClient studentClient;

    @Override
    @Transactional
    @CircuitBreaker(name = "complianceApi", fallbackMethod = "fallbackGenerate")
    public void generateCompliance(Long officerId) {
        logger.info("--- STARTING COMPLIANCE SCAN ---");
        
        RemoteUserDto officer = userClient.getUserById(officerId);
        if (officer == null) throw new ResourceNotFoundException("Officer not found: " + officerId);

        // 1. GRANT COMPLIANCE (With Null Safety)
        try {
            List<GrantApplicationDto> approvedApps = researchAndGrantClient.getGrantApplicationsByStatus(Arrays.asList("APPROVED", "COMPLETED"));
            if (approvedApps != null) {
                for (GrantApplicationDto app : approvedApps) {
                    // CRITICAL FIX: If ProjectID is null, skip it to prevent DB crash
                    if (app.getProjectId() == null) {
                        logger.warn("Skipping app: Project ID is null.");
                        continue; 
                    }
                    
                    try {
                        GrantDto grant = researchAndGrantClient.getGrantByProjectId(app.getProjectId());
                        if (grant == null) {
                            saveInternalCompliance(app.getProjectId(), "PROJECT", "Missing Grant Record", officer);
                        }
                    } catch (Exception e) {
                        saveInternalCompliance(app.getProjectId(), "PROJECT", "Grant not found (404/Error)", officer);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Grant scan failed: {}", e.getMessage());
        }

        // 2. ENROLLMENT & PROGRAM COMPLIANCE (Link-based logic)
        try {
            List<RemoteProgramDto> inactivePrograms = programClient.getByStatus("INACTIVE");
            if (inactivePrograms != null && !inactivePrograms.isEmpty()) {
                
                List<RemoteCourseDto> allCourses = programClient.getAllCourses(); 
                List<RemoteEnrollmentDto> allEnrollments = programClient.getAllEnrollments();

                for (RemoteProgramDto prog : inactivePrograms) {
                    logger.info("Checking Inactive Program ID: {}", prog.getProgramID());

                    // Step A: Find all courses belonging to this inactive program
                    List<Long> coursesInProgram = allCourses.stream()
                        .filter(c -> c.getProgramId() != null && c.getProgramId().equals(prog.getProgramID()))
                        .map(RemoteCourseDto::getCourseId)
                        .collect(Collectors.toList());

                    // Step B: Check if anyone is enrolled in those courses
                    boolean hasTrappedStudents = allEnrollments.stream()
                        .anyMatch(e -> e.getCourseId() != null && coursesInProgram.contains(e.getCourseId()));

                    if (hasTrappedStudents) {
                        saveInternalCompliance(prog.getProgramID(), "PROGRAM", "Students still enrolled in courses of this inactive program", officer);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Enrollment scan failed: {}. Check paths in RemoteProgramClient.", e.getMessage());
        }
        
        logger.info("--- COMPLIANCE SCAN FINISHED ---");
    }

    private void saveInternalCompliance(Long entityId, String type, String notes, RemoteUserDto officer) {
        if (entityId == null) return;

        if (!complianceRepo.existsByEntityIdAndEntityType(entityId, type)) {
            ComplianceRecord record = new ComplianceRecord();
            record.setEntityId(entityId); 
            record.setEntityType(type);
            record.setNotes(notes); 
            record.setDate(LocalDate.now());
            record.setOfficerId(officer.getUserId()); 
            record.setResult("UNDER_REVIEW");
            complianceRepo.save(record);
            logger.info("Violation Recorded: {} ID {}", type, entityId);
        }
    }

    @Override
    public List<ComplianceRecordDTO> getAllCompliance() {
        return complianceRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public ComplianceRecordDTO getComplianceById(Long id) {
        ComplianceRecord record = complianceRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        return convertToDTO(record);
    }

    @Override
    @Transactional
    public void deleteCompliance(Long id) {
        complianceRepo.deleteById(id);
    }

    @Override
    @Transactional
    public ComplianceRecordDTO createManual(ComplianceRecord record, Long officerId) {
        record.setOfficerId(officerId);
        record.setDate(LocalDate.now());
        return convertToDTO(complianceRepo.save(record));
    }

    @Override
    @Transactional
    public ComplianceRecordDTO updateCompliance(Long id, ComplianceRecord details) {
        ComplianceRecord existing = complianceRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        existing.setNotes(details.getNotes());
        existing.setResult(details.getResult());
        return convertToDTO(complianceRepo.save(existing));
    }

    public void fallbackGenerate(Long officerId, Throwable t) {
        logger.error("Circuit Breaker active: {}", t.getMessage());
        throw new RuntimeException("Compliance Scan Failed: " + t.getMessage());
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
        return dto;
    }
}