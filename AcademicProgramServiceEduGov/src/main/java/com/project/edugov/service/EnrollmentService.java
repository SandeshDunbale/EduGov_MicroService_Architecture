package com.project.edugov.service;

import java.util.List;

import com.project.edugov.dto.EnrollmentResponseDTO;
import com.project.edugov.model.Status;

public interface EnrollmentService {
	EnrollmentResponseDTO applyForCourse(Long studentId, Long courseId);

	List<EnrollmentResponseDTO> getEnrollmentsByStatus(Status status);

	EnrollmentResponseDTO updateEnrollmentStatus(Long enrollmentId, Long adminId, Status newStatus);

	List<EnrollmentResponseDTO> getAllEnrollments();
}