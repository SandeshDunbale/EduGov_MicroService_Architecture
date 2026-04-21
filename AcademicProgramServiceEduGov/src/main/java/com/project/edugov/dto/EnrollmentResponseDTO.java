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

	// From Internal Academic DB (Enrollment Table)
	private Long enrollmentId;
	private LocalDateTime enrollmentDate;
	private Status status;

	// From Internal Academic DB (Course Table/Join)
	private Long courseId;
	private String courseTitle;

	// From External STUDENT-SERVICE (via studentId)
	private Long studentId; // Added this to identify the student
	private String studentName;
	private String studentEmail;

	// From External FACULTY-SERVICE (via facultyId linked to the course)
	private Long facultyId;
	private String facultyName; // Corrected to camelCase

	// From External IDENTITY-SERVICE (via approvedByAdminId)
	private Long approvedByAdminId;
	private String approvedByAdminName;
}