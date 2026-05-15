package com.project.edugov.service;

import com.project.edugov.dto.ReportDTO;
import com.project.edugov.model.ReportScope;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    ReportDTO generateReportByScope(ReportScope scope);

    // ✅ THIS METHOD MUST EXIST
    ReportDTO generateReport(ReportDTO dto);

    List<ReportDTO> getAllReports();

    List<ReportDTO> getReportsByScope(String scope);

    List<ReportDTO> getReportsByDateRange(LocalDate start, LocalDate end);
}