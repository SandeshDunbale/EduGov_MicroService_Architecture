package com.project.edugov.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ProjectClient projectClient;
    private final GrantClient grantClient;
    private final ProgramClient programClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🔁 DTO → ENTITY
    private Report convertToEntity(ReportDTO dto) {
        Report report = new Report();
        report.setReportId(dto.getReportId());
        if (dto.getScope() != null) { 
            report.setScope(ReportScope.valueOf(dto.getScope().toUpperCase())); 
        }
        report.setMetrics(dto.getMetrics());
        if (dto.getGeneratedDate() != null) { 
            report.setGeneratedDate(dto.getGeneratedDate().atStartOfDay()); 
        }
        return report;
    }

    // 🔁 ENTITY → DTO
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

    @Override
    public ReportDTO generateReportByScope(ReportScope scope) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            if (scope == ReportScope.PROGRAM) {
                metrics.put("totalPrograms", programClient.getTotalPrograms());
                metrics.put("activePrograms", programClient.getProgramCountByStatus("ACTIVE"));
                metrics.put("inactivePrograms", programClient.getProgramCountByStatus("INACTIVE"));

            } else if (scope == ReportScope.PROJECT) {
                metrics.put("totalProjects", projectClient.getTotalProjects());
                metrics.put("completedProjects", projectClient.getProjectCountByStatus("COMPLETED"));

                long inProgress = projectClient.getProjectCountByStatus("SUBMITTED") + 
                                  projectClient.getProjectCountByStatus("UNDER_REVIEW");
                metrics.put("inProgressProjects", inProgress);

            } else if (scope == ReportScope.GRANT) {
                metrics.put("totalGrantCount", grantClient.getTotalGrants());
                Double totalMoney = grantClient.getTotalGrantAmount();
                metrics.put("totalGrantAmount", totalMoney != null ? totalMoney : 0.0);
            }
        } catch (Exception e) {
            log.error("Error communicating with microservices for scope {}: {}", scope, e.getMessage());
            // Depending on your requirements, you might want to rethrow a custom Feign communication exception here
        }

        Report report = new Report();
        report.setScope(scope);
        report.setGeneratedDate(LocalDateTime.now());

        try {
            report.setMetrics(objectMapper.writeValueAsString(metrics));
        } catch (JsonProcessingException e) {
            report.setMetrics("{}");
            log.error("Failed to parse metrics to JSON", e);
        }

        return convertToDTO(reportRepository.save(report));
    }

    @Override
    public ReportDTO generateReport(ReportDTO dto) {
        Report report = convertToEntity(dto);
        report.setGeneratedDate(LocalDateTime.now());
        return convertToDTO(reportRepository.save(report));
    }

    @Override
    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportDTO> getReportsByScope(String scope) {
        ReportScope enumScope = ReportScope.valueOf(scope.toUpperCase());
        return reportRepository.findByScope(enumScope).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportDTO> getReportsByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startOfDay = start.atStartOfDay();
        LocalDateTime endOfDay = end.atTime(23, 59, 59);

        return reportRepository.findByGeneratedDateBetween(startOfDay, endOfDay).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}