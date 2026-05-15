package com.project.edugov.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.GrantApplication;
import com.project.edugov.model.GrantApplicationStatus;
import com.project.edugov.model.ResearchProject;

@Repository
public interface GrantApplicationRepository extends JpaRepository<GrantApplication, Long> {

	List<GrantApplication> findByStatus(GrantApplicationStatus status);

	Optional<GrantApplication> findByProject_ProjectId(Long projectId);

	// CHANGED: Removed the underscore, it now directly queries the Long facultyId field
    List<GrantApplication> findByFacultyId(Long facultyId);
	
	List<GrantApplication> findByStatusIn(List<GrantApplicationStatus> statuses);
    
    Optional<GrantApplication> findByProject(ResearchProject project);
    
    List<GrantApplication> findByReviewedByUserIdAndStatusIn(Long userId, List<GrantApplicationStatus> statuses);
}