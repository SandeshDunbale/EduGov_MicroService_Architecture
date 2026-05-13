package com.project.edugov.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edugov.client.GrantClient;
import com.project.edugov.client.ProgramClient;
import com.project.edugov.client.ProjectClient;
import com.project.edugov.dto.ReportDTO;
import com.project.edugov.model.Report;
import com.project.edugov.model.ReportScope;
import com.project.edugov.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ProjectClient projectClient;
    private final GrantClient grantClient;
    private final ProgramClient programClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /* ================= ENTITY → DTO ================= */
    private ReportDTO convertToDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setReportId(report.getReportId());
        dto.setScope(report.getScope().name());
        dto.setMetrics(report.getMetrics());
        dto.setGeneratedDate(report.getGeneratedDate().toLocalDate());
        return dto;
    }

    /* ================= DTO → ENTITY ================= */
    private Report convertToEntity(ReportDTO dto) {
        Report report = new Report();
        report.setReportId(dto.getReportId());
        report.setScope(ReportScope.valueOf(dto.getScope().toUpperCase()));
        report.setMetrics(dto.getMetrics());
        report.setGeneratedDate(dto.getGeneratedDate().atStartOfDay());
        return report;
    }

    /* ================= REQUIRED METHOD ================= */
    @Override
    public ReportDTO generateReport(ReportDTO dto) {
        Report report = convertToEntity(dto);
        report.setGeneratedDate(LocalDateTime.now());
        return convertToDTO(reportRepository.save(report));
    }

    /* ================= GENERATE BY SCOPE ================= */
    @Override
    public ReportDTO generateReportByScope(ReportScope scope) {

        Map<String, Object> metrics = new HashMap<>();

        try {
            if (scope == ReportScope.PROGRAM) {
                metrics.put("totalPrograms", programClient.getTotalPrograms());
            } 
            else if (scope == ReportScope.PROJECT) {
                metrics.put("totalProjects", projectClient.getTotalProjects());
            } 
            else if (scope == ReportScope.GRANT) {
                metrics.put("totalGrants", grantClient.getTotalGrants());
            }
        } catch (Exception e) {
            log.error("Error fetching metrics for scope {}", scope, e);
        }

        Report report = new Report();
        report.setScope(scope);
        report.setGeneratedDate(LocalDateTime.now());

        try {
            report.setMetrics(objectMapper.writeValueAsString(metrics));
        } catch (JsonProcessingException e) {
            report.setMetrics("{}");
        }

        return convertToDTO(reportRepository.save(report));
    }

    /* ================= FETCH METHODS ================= */
    @Override
    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportDTO> getReportsByScope(String scope) {
        return reportRepository
                .findByScope(ReportScope.valueOf(scope.toUpperCase()))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportDTO> getReportsByDateRange(LocalDate start, LocalDate end) {
        return reportRepository
                .findByGeneratedDateBetween(
                        start.atStartOfDay(),
                        end.atTime(23, 59, 59))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
