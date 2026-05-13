package com.project.edugov.service;

import java.util.List;

import com.project.edugov.dto.CourseDTO;
import com.project.edugov.model.Course;

public interface CourseService {

	CourseDTO createCourse(Course course);

	List<CourseDTO> getCoursesByFacultyId(Long facultyId);

	List<CourseDTO> getCoursesByProgramId(Long programId);

	CourseDTO getCourseById(Long courseId);

	CourseDTO updateCourse(Long id, Course details);

	List<CourseDTO> getAllCourses();
}