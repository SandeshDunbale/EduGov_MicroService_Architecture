package com.project.edugov.service;

import com.project.edugov.dto.ComplianceRecordDTO;
import com.project.edugov.model.ComplianceRecord;
import java.util.List;

public interface ComplianceService {

    void generateCompliance(Long officerId);

    ComplianceRecordDTO createManual(ComplianceRecord record, Long officerId);

    List<ComplianceRecordDTO> getAllCompliance();

    ComplianceRecordDTO updateCompliance(Long id, ComplianceRecord details);

    void deleteCompliance(Long id);
}