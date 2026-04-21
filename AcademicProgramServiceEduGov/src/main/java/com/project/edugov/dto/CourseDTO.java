package com.project.edugov.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.project.edugov.model.Status;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "courseId", "title", "description", "status", "programId", "programTitle", "programStatus",
		"facultyId", "facultyName", "facultyEmail", "adminId", "adminName" })
public class CourseDTO {

	// From Internal Academic DB (Course Table)
	private Long courseId; // Strict camelCase naming
	private String title;
	private String description;
	private Status status;

	// From Internal Academic DB (Program Table)
	private Long programId;
	private String programTitle;
	private String programStatus;

	// From External FACULTY-SERVICE (or Identity Service)
	private Long facultyId;
	private String facultyName;
	private String facultyEmail;

	// From External IDENTITY-SERVICE (User Table)
	private Long adminId;
	private String adminName;
}