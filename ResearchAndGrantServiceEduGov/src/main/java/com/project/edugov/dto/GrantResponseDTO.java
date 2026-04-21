package com.project.edugov.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.edugov.model.GrantStatus;

import lombok.Data;

@Data
public class GrantResponseDTO {
    private Long grantId;
    private String projectTitle;
    private BigDecimal amount;
    private LocalDate date;
    private GrantStatus status;
   // private String approvedByName; 
    private String approvedByRole;
}
