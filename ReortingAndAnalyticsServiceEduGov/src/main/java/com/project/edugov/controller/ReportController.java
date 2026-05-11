package com.project.edugov.controller;
 
import com.project.edugov.dto.ReportDTO;
import com.project.edugov.model.ReportScope;
import com.project.edugov.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDate;
import java.util.List;
 
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor

public class ReportController {
 
    private final ReportService reportService;
 
    @PostMapping("/generate")
    public ReportDTO generate(@RequestParam ReportScope scope) {
        return reportService.generateReportByScope(scope);
    }
 
    @GetMapping            //http://localhost:8081/api/reports
    public List<ReportDTO> getAll() {
        return reportService.getAllReports();
    }
 
    @GetMapping("/scope/{scope}")
    public List<ReportDTO> getByScope(@PathVariable String scope) {
        return reportService.getReportsByScope(scope);
    }
 
    @GetMapping("/date-range")
    public List<ReportDTO> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
 
        return reportService.getReportsByDateRange(start, end);
    }
}
 