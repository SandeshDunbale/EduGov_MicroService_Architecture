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

	private Long courseId;
	private String title;
	private String description;
	private Status status;

	private Long programId;
	private String programTitle;
	private String programStatus;

	private Long facultyId;
	private String facultyName;
	private String facultyEmail;

	private Long adminId;
	private String adminName;
}