package com.project.edugov.dto;

import lombok.Data;

@Data
public class ProjectUpdateResponseDTO {
	private Long projectId;
	private String title;
	private String description;
	private FacultyMinimalDTO faculty;
}