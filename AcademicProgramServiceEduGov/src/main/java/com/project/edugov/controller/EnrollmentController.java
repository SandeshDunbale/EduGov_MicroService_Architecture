package com.project.edugov.controller;
 
import java.util.List;
import java.util.Map;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/enrollments")
public class EnrollmentController {

	private final EnrollmentService enrollmentService;

	// Apply for enrollments
	@PostMapping("/apply")
	public ResponseEntity<EnrollmentResponseDTO> apply(@RequestBody Map<String, Long> request) {
		log.info("POST: creating new enrollment application for student: {}", request.get("studentId"));
		EnrollmentResponseDTO response = enrollmentService.applyForCourse(request.get("studentId"),
				request.get("courseId"));
		log.info("POST: enrollment created successfully with id {}", response.getEnrollmentId());
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
 
	// Fetch enrollments based on status
	@GetMapping("/status/{status}")
	public ResponseEntity<List<EnrollmentResponseDTO>> getByStatus(@PathVariable Status status) {
		log.info("GET: fetching enrollments with status: {}", status);
		List<EnrollmentResponseDTO> results = enrollmentService.getEnrollmentsByStatus(status);
		log.info("GET: getting {} enrollments with status {}", results.size(), status);
		return ResponseEntity.ok(results);
	}
 
	// Update enrollments status(APPROVE/REJECT)
	@PutMapping("/update-status")
	public ResponseEntity<EnrollmentResponseDTO> updateEnrollment(@RequestBody Map<String, Object> data) {
		log.info("PATCH: updating enrollment status for id: {}", data.get("enrollmentId"));
		EnrollmentResponseDTO result = enrollmentService.updateEnrollmentStatus(data);
		log.info("PATCH: enrollment {} updated successfully", result.getEnrollmentId());
		return ResponseEntity.ok(result);
	}
 
	// List of all enrollments
	@GetMapping("/all")
	public ResponseEntity<List<EnrollmentResponseDTO>> getAll() {
		log.info("GET: fetching full list of enrollments...");
		List<EnrollmentResponseDTO> results = enrollmentService.getAllEnrollments();
		log.info("GET: Total {} enrollments found", results.size());
		return ResponseEntity.ok(results);
	}
}