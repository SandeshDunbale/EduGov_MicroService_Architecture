package com.project.edugov.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportDTO {

    private Long reportId;
    private String scope;
    private String metrics;
    private LocalDate generatedDate;
}