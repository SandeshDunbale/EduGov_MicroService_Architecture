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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

	private final CourseService courseService;

	// Create a new academic course
	@PostMapping("/save")
	public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody Course course) {
		log.info("POST Request :- /courses/save || Action: Initiating Course Creation for '{}'", course.getTitle());
		CourseDTO result = courseService.createCourse(course);
		log.info("Status: Created || Course ID: {}", result.getCourseId());
		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	// Fetch a specific course details by CourseId
	@GetMapping("/{courseId}")
	public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long courseId) {
		log.info("GET Request:- /courses/{} || Action: Fetching Course Details", courseId);
		CourseDTO result = courseService.getCourseById(courseId);
		log.info("Status: Found || Course ID: {}", result.getCourseId());
		return ResponseEntity.ok(result);
	}

	// Fetch a specific course details assigned to facultyId
	@GetMapping("/faculty/{facultyId}")
	public ResponseEntity<List<CourseDTO>> getCoursesByFaculty(@PathVariable Long facultyId) {
		log.info("GET Request:- /courses/faculty/{} || Action: Fetching Courses for Faculty", facultyId);
		List<CourseDTO> courses = courseService.getCoursesByFacultyId(facultyId);
		log.info("Status: Success || Records Found: {}", courses.size());
		return ResponseEntity.ok(courses);
	}

	// Fetch course details under specififc programId
	@GetMapping("/program/{programId}")
	public ResponseEntity<List<CourseDTO>> getCoursesByProgram(@PathVariable Long programId) {
		log.info("GET Request:- /courses/program/{} || Action: Fetching Courses for Program", programId);
		List<CourseDTO> courses = courseService.getCoursesByProgramId(programId);
		log.info("Status: Success || Records Found: {}", courses.size());
		return ResponseEntity.ok(courses);
	}

	// Update an existing course
	@PatchMapping("/update/{id}")
	public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @RequestBody Course details) {
		log.info("PATCH Request:- /courses/update/{} || Action: Updating Course Details", id);
		CourseDTO updated = courseService.updateCourse(id, details);
		log.info("Status: Updated || Course ID: {}", updated.getCourseId());
		return ResponseEntity.ok(updated);
	}

	// List of all courses
	@GetMapping("/all")
	public ResponseEntity<List<CourseDTO>> getAllCourses() {
		log.info("GET Request:- /courses/all || Action: Retrieving All Academic Courses");
		List<CourseDTO> courses = courseService.getAllCourses();
		log.info("Status: Success || Total Courses: {}", courses.size());
		return ResponseEntity.ok(courses);
	}
}