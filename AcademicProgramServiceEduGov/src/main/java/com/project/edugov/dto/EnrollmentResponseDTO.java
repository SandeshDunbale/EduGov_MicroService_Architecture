package com.project.edugov.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.project.edugov.model.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "enrollmentId", "enrollmentDate", "status", "courseId", "courseTitle", "studentId", "studentName",
		"studentEmail", "facultyId", "facultyName", "approvedByAdminId", "approvedByAdminName" })
public class EnrollmentResponseDTO {

	private Long enrollmentId;
	private LocalDateTime enrollmentDate;
	private Status status;

	private Long courseId;
	private String courseTitle;

	private Long studentId;
	private String studentName;
	private String studentEmail;

	private Long facultyId;
	private String facultyName;

	private Long approvedByAdminId;
	private String approvedByAdminName;
}