package com.project.edugov.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Grant;
import com.project.edugov.model.ResearchProject;

@Repository
public interface GrantRepository extends JpaRepository<Grant, Long> {

	Optional<Grant> findByProject_ProjectId(Long projectId);

	List<Grant> findByFacultyId(Long facultyId);
	
	Optional<Grant> findByProject(ResearchProject project);
	@Query("SELECT SUM(g.amount) FROM Grant g")
    Double sumTotalGrantAmount();
}
