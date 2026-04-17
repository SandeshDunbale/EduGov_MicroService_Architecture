package com.project.edugov.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.EnrollmentResponseDTO;
import com.project.edugov.model.Status;
import com.project.edugov.service.EnrollmentService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/enrollments")
@Slf4j
public class EnrollmentController {

	@Autowired
	private EnrollmentService enrollmentService;

	/**
	 * Student apply for a course. Expected JSON Body: { "studentId": 1, "courseId":
	 * 5 }
	 */
	@PostMapping("/apply")
	public ResponseEntity<EnrollmentResponseDTO> apply(@RequestBody Map<String, Long> request) {
		Long studentId = request.get("studentId");
		Long courseId = request.get("courseId");

		log.info("REST Request: Student ID {} applying for Course ID {}", studentId, courseId);

		EnrollmentResponseDTO response = enrollmentService.applyForCourse(studentId, courseId);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	/**
	 * Admin updates enrollment status (Approved/Rejected). Path:
	 * /enrollments/update/1/admin/10/status/APPROVED
	 */
	@PutMapping("/update/{eId}/admin/{aId}/status/{status}")
	public ResponseEntity<EnrollmentResponseDTO> updateEnrollment(@PathVariable Long eId, @PathVariable Long aId,
			@PathVariable Status status) {

		log.info("REST Request: Admin {} updating Enrollment {} status to {}", aId, eId, status);

		EnrollmentResponseDTO response = enrollmentService.updateEnrollmentStatus(eId, aId, status);
		return ResponseEntity.ok(response);
	}

	/**
	 * Filter enrollments by status (PENDING, APPROVED, REJECTED).
	 */
	@GetMapping("/status/{status}")
	public ResponseEntity<List<EnrollmentResponseDTO>> getByStatus(@PathVariable Status status) {
		log.info("REST Request: Fetching enrollments with status: {}", status);
		return ResponseEntity.ok(enrollmentService.getEnrollmentsByStatus(status));
	}

	/**
	 * Fetch all enrollment records.
	 */
	@GetMapping("/all")
	public ResponseEntity<List<EnrollmentResponseDTO>> getAll() {
		log.info("REST Request: Fetching all enrollment records");
		return ResponseEntity.ok(enrollmentService.getAllEnrollments());
	}
}