package com.project.edugov.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.CourseDTO;
import com.project.edugov.model.Course;
import com.project.edugov.service.CourseService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/courses")
@Slf4j
public class CourseController {

	@Autowired
	private CourseService courseService;

	/**
	 * Admin create new course. Updated to extract flat IDs from the request body.
	 */
	@PostMapping("/save")
	public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody Course course) {
		// Extraction logic changed for Microservices (flat fields)
		Long pId = (course.getProgram() != null) ? course.getProgram().getProgramId() : null;
		Long fId = course.getFacultyId();
		Long aId = course.getCreatedByAdminId();

		log.info("REST Request: Create course '{}' by Admin ID: {} for Faculty: {} in Program: {}", course.getTitle(),
				aId, fId, pId);

		return new ResponseEntity<>(courseService.createCourse(course, pId, fId, aId), HttpStatus.CREATED);
	}

	// Get all courses
	@GetMapping("/all")
	public ResponseEntity<List<CourseDTO>> getAllCourses() {
		log.info("REST Request: Fetch all courses");
		return ResponseEntity.ok(courseService.getAllCourses());
	}

	// Find course by faculty Id (External Service verification happens in
	// ServiceImpl)
	@GetMapping("/faculty/{facultyId}")
	public ResponseEntity<List<CourseDTO>> getCoursesByFaculty(@PathVariable Long facultyId) {
		log.info("REST Request: Fetch courses for Faculty ID: {}", facultyId);
		return ResponseEntity.ok(courseService.getCoursesByFacultyId(facultyId));
	}

	// Find courses by program Id (Local DB check)
	@GetMapping("/program/{programId}")
	public ResponseEntity<List<CourseDTO>> getCoursesByProgram(@PathVariable Long programId) {
		log.info("REST Request: Fetch courses for Program ID: {}", programId);
		return ResponseEntity.ok(courseService.getCoursesByProgramId(programId));
	}

	// Find courses by course Id
	@GetMapping("/{courseId}")
	public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long courseId) {
		log.info("REST Request: Fetching course details for ID: {}", courseId);
		return ResponseEntity.ok(courseService.getCourseById(courseId));
	}

	// Update course details
	@PatchMapping("/update/{id}")
	public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @RequestBody Course details) {
		log.info("REST Request: Update Course ID: {}", id);
		return ResponseEntity.ok(courseService.updateCourse(id, details));
	}
}