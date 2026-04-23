package com.project.edugov.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.project.edugov.model.Status;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "programId", "title", "description", "startDate", "endDate", "status", "adminId", "adminName",
		"adminEmail" })
public class ProgramDTO {

	private Long programId;
	private String title;
	private String description;
	private LocalDate startDate;
	private LocalDate endDate;
	private Status status;

	private Long adminId;
	private String adminName;
	private String adminEmail;
}