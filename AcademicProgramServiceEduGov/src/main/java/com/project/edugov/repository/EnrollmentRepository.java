package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Enrollment;
import com.project.edugov.model.Status;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	// Matches your monolith logic: checks if a student is already enrolled in a
	// course
	// Uses the decoupled Long studentId and the internal Course relationship
	boolean existsByStudentIdAndCourse_CourseId(Long studentId, Long courseId);

	// Matches your monolith logic: filters by PENDING, ACTIVE, etc.
	List<Enrollment> findByStatus(Status status);
}