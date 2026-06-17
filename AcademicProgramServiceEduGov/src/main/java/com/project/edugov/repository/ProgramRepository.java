package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Program;
import com.project.edugov.model.Status;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

	// Check if program title exists
	boolean existsByTitleIgnoreCase(String title);

	// Search programs by title
	List<Program> findByTitleContainingIgnoreCase(String title);

	// Module 6 requirement
	List<Program> findByStatus(Status status);
}