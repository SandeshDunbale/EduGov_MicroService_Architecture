package com.project.edugov.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.ProjectStatus;
import com.project.edugov.model.ResearchProject;

@Repository
public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {
 
	List<ResearchProject> findByFacultyId(Long facultyId);

	List<ResearchProject> findByStatus(ProjectStatus status);

	// CHANGED: Removed the underscore here too - existsByTitleAndFaculty_FacultyId
    boolean existsByTitleAndFacultyId(String title, Long facultyId);
    
    long countByStatus(ProjectStatus status);
}






















// 1. To show the Faculty their own projects on their dashboard
//2. To allow Program Managers to filter projects by status (e.g., APPROVED)

//user defined method following dsl grammer 
// 3. Check if a project title already exists for a faculty member