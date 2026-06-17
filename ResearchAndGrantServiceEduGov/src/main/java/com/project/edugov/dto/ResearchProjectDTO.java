package com.project.edugov.dto;

import java.time.LocalDate;

import com.project.edugov.model.ProjectStatus;

import lombok.Data;

@Data
public class ResearchProjectDTO {
	private Long projectId;
	private String title;
	private String description;
	private FacultyMinimalDTO faculty;
	private LocalDate startDate;
	private LocalDate endDate;
	private ProjectStatus status;
}
