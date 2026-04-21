package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

	// Title check
	boolean existsByTitleIgnoreCase(String title);

	// Fetch by Faculty ID
	List<Course> findByFacultyId(Long facultyId);

	// Fetch by ProgramId
	List<Course> findByProgram_ProgramId(Long programId);

	// Unique title check within one program
	boolean existsByTitleIgnoreCaseAndProgram_ProgramId(String title, Long programId);
}