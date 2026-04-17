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

	// Local Academic Service Data
	private Long programId; // Corrected to camelCase
	private String title;
	private String description;
	private LocalDate startDate;
	private LocalDate endDate;
	private Status status;

	// External Identity Service Data (fetched via Feign Client later)
	private Long adminId;
	private String adminName; // Renamed for clarity (Admin's name)
	private String adminEmail; // Renamed for clarity (Admin's email)
}