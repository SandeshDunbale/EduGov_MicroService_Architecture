package com.project.edugov.service;
 
import com.project.edugov.dto.ReportDTO;
import com.project.edugov.exception.BadRequestException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Report;
import com.project.edugov.model.ReportScope;
import com.project.edugov.repository.ReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
 
import org.springframework.stereotype.Service;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
 
@Service
public class ReportServiceImpl implements ReportService {
 
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }
 
    // ================= DTO → ENTITY =================
    private Report convertToEntity(ReportDTO dto) {
        Report report = new Report();
 
        report.setReportId(dto.getReportId());
 
        if (dto.getScope() != null) {
            try {
                report.setScope(ReportScope.valueOf(dto.getScope().toUpperCase()));
            } catch (Exception e) {
                throw new BadRequestException("Invalid scope: " + dto.getScope());
            }
        }
 
        report.setMetrics(dto.getMetrics());
 
        if (dto.getGeneratedDate() != null) {
            report.setGeneratedDate(dto.getGeneratedDate().atStartOfDay());
        }
 
        return report;
    }
 
    // ================= ENTITY → DTO =================
    private ReportDTO convertToDTO(Report report) {
        ReportDTO dto = new ReportDTO();
 
        dto.setReportId(report.getReportId());
 
        if (report.getScope() != null) {
            dto.setScope(report.getScope().name());
        }
 
        dto.setMetrics(report.getMetrics());
 
        if (report.getGeneratedDate() != null) {
            dto.setGeneratedDate(report.getGeneratedDate().toLocalDate());
        }
 
        return dto;
    }
 
    // ================= GENERATE REPORT =================
    @Override
    public ReportDTO generateReportByScope(ReportScope scope) {
 
        Map<String, Object> metrics = new HashMap<>();
 
        // 👉 Dummy metrics (you can replace later with real logic)
        metrics.put("totalRecords", reportRepository.count());
        metrics.put("generatedFor", scope.name());
 
        Report report = new Report();
        report.setScope(scope);
        report.setGeneratedDate(LocalDateTime.now());
 
        try {
            report.setMetrics(objectMapper.writeValueAsString(metrics));
        } catch (Exception e) {
            report.setMetrics("{}");
        }
 
        return convertToDTO(reportRepository.save(report));
    }
 
    // ================= CREATE REPORT =================
    @Override
    public ReportDTO generateReport(ReportDTO dto) {
        Report report = convertToEntity(dto);
        report.setGeneratedDate(LocalDateTime.now());
 
        return convertToDTO(reportRepository.save(report));
    }
 
    // ================= GET ALL =================
    @Override
    public List<ReportDTO> getAllReports() {
 
        List<Report> reports = reportRepository.findAll();
 
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found");
        }
 
        return reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
 
    // ================= GET BY SCOPE =================
    @Override
    public List<ReportDTO> getReportsByScope(String scope) {
 
        ReportScope enumScope;
 
        try {
            enumScope = ReportScope.valueOf(scope.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid scope: " + scope);
        }
 
        List<Report> reports = reportRepository.findByScope(enumScope);
 
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found for scope: " + scope);
        }
 
        return reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
 
    // ================= GET BY DATE RANGE =================
    @Override
    public List<ReportDTO> getReportsByDateRange(LocalDate start, LocalDate end) {
 
        if (start == null || end == null) {
            throw new BadRequestException("Start date and End date must not be null");
        }
 
        if (start.isAfter(end)) {
            throw new BadRequestException("Start date cannot be after End date");
        }
 
        LocalDateTime startOfDay = start.atStartOfDay();
        LocalDateTime endOfDay = end.atTime(23, 59, 59);
 
        List<Report> reports =
                reportRepository.findByGeneratedDateBetween(startOfDay, endOfDay);
 
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found between given dates");
        }
 
        return reports.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
 