package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Enrollment;
import com.project.edugov.model.Status;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	// Checks for duplicate enrollment
	boolean existsByStudentIdAndCourse_CourseId(Long studentId, Long courseId);

	// Find enrollments by status
	List<Enrollment> findByStatus(Status status);
	
	
}