package com.project.edugov.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.edugov.model.GrantApplicationStatus;

import lombok.Data;

@Data
public class GrantApplicationDTO {
	private Long applicationID;
	private Long projectId;
	private String projectTitle;
	private FacultyMinimalDTO faculty;
	private BigDecimal requestedAmount;
	private GrantApplicationStatus status;
	private LocalDate submittedDate;
}
