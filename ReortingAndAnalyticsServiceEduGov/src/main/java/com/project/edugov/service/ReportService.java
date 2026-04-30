package com.project.edugov.service;

import java.time.LocalDate;
import java.util.List;

import com.project.edugov.dto.ReportDTO;
import com.project.edugov.model.ReportScope;

public interface ReportService {

	ReportDTO generateReport(ReportDTO dto);

	ReportDTO generateReportByScope(ReportScope scope);

	List<ReportDTO> getAllReports();

	List<ReportDTO> getReportsByScope(String scope);

	List<ReportDTO> getReportsByDateRange(LocalDate start, LocalDate end);

}
/*
 * import com.project.edugov.dto.ReportDTO; import
 * com.project.edugov.model.ReportScope;
 * 
 * import java.time.LocalDate; import java.util.List;
 * 
 * public interface ReportService { ReportDTO generateReport(ReportDTO dto);
 * 
 * ReportDTO generateReportByScope(ReportScope scope);
 * 
 * List<ReportDTO> getAllReports();
 * 
 * List<ReportDTO> getReportsByScope(String scope);
 * 
 * List<ReportDTO> getReportsByDateRange(LocalDate start, LocalDate end); }
 */