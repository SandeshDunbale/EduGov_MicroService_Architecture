package com.project.edugov.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.ProjectUpdateResponseDTO;
import com.project.edugov.dto.ResearchProjectDTO;
import com.project.edugov.model.ResearchProject;
import com.project.edugov.service.ResearchProjectService;
//import com.project.edugov.service.AuditServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//@CrossOrigin(origins = "http://localhost:5173")	
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j

public class ResearchProjectController {

	private final ResearchProjectService projectService;
	//private final AuditServiceImpl auditService;

	@PostMapping("/{facultyId}")
	public ResponseEntity<ResearchProjectDTO> createProject(@Valid @RequestBody ResearchProject project,
			@PathVariable Long facultyId) {

	//	auditService.logAction("CREATE_RESEARCH_PROJECT", "FACULTY_ID_" + facultyId);

		log.info("API Hit: POST /api/projects/{} | Creating project: '{}'", facultyId, project.getTitle());

		ResearchProjectDTO createdProject = projectService.createProject(project, facultyId); // calls service to save
																								// the prj

		log.info("Project created successfully with ID: {}", createdProject.getProjectId());
		return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
	}

	@GetMapping("/faculty/{facultyId}")
	public ResponseEntity<List<ResearchProjectDTO>> getProjectsByFaculty(@PathVariable Long facultyId) {
		log.info("API Hit: GET /api/projects/faculty/{} | Fetching all faculty projects", facultyId);

		List<ResearchProjectDTO> projects = projectService.getProjectsByFaculty(facultyId);
		log.debug("Found {} projects for Faculty ID: {}", projects.size(), facultyId);

		return ResponseEntity.ok(projects);
	}

	@GetMapping("/{projectId}")
	public ResponseEntity<ResearchProjectDTO> getProjectById(@PathVariable Long projectId) {
		log.info("API Hit: GET /api/projects/{} | Fetching project details", projectId);
		return ResponseEntity.ok(projectService.getProjectById(projectId));
	}

	@PutMapping("/{projectId}")
	public ResponseEntity<ProjectUpdateResponseDTO> updateProject(@PathVariable Long projectId,
			@Valid @RequestBody ResearchProject projectDetails) {

		//auditService.logAction("UPDATE_RESEARCH_PROJECT", "PROJECT_ID_" + projectId);

		log.info("API Hit: PUT /api/projects/{} | Attempting update for project: '{}'", projectId,
				projectDetails.getTitle());

		ProjectUpdateResponseDTO updatedProject = projectService.updateProject(projectId, projectDetails);

		log.info("Update successful for Project ID: {}", projectId);
		return ResponseEntity.ok(updatedProject);
	}
}