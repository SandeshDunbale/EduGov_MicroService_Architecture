package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

	// Check if course title exists globally
	boolean existsByTitleIgnoreCase(String title);

	// REFACTORED: facultyId is now a direct Long field in the Course entity
	List<Course> findByFacultyId(Long facultyId);

	// REFACTORED: Matches the updated 'programId' field name in Program entity
	List<Course> findByProgram_ProgramId(Long programId);

	// REFACTORED: Combined unique check using corrected camelCase
	boolean existsByTitleIgnoreCaseAndProgram_ProgramId(String title, Long programId);
}