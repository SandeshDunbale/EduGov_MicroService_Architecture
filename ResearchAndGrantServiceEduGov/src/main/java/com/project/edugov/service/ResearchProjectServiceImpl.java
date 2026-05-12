package com.project.edugov.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.FacultyClient;
import com.project.edugov.dto.FacultyMinimalDTO;
import com.project.edugov.dto.ProjectUpdateResponseDTO;
import com.project.edugov.dto.ResearchProjectDTO;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.GrantApplicationStatus;
import com.project.edugov.model.ProjectStatus;
import com.project.edugov.model.ResearchProject;
import com.project.edugov.repository.GrantApplicationRepository;
import com.project.edugov.repository.ResearchProjectRepository;

// ADDED: Resilience4j Import
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchProjectServiceImpl implements ResearchProjectService {

	private final ResearchProjectRepository projectRepository; 
	private final GrantApplicationRepository applicationRepository;

	private final ModelMapper modelMapper;

	

	private final FacultyClient facultyClient;

	// 1. INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

	@Override
	@Transactional
	@CircuitBreaker(name = "facultyServiceCb", fallbackMethod = "createProjectFallback")
	public ResearchProjectDTO createProject(ResearchProject project, Long facultyId) {
		log.info("Attempting to create a new project: '{}' for Faculty ID: {}", project.getTitle(), facultyId);

		FacultyMinimalDTO faculty;
		try {
			faculty = facultyClient.getFacultyById(facultyId);
		} catch (Exception e) {

			log.error("Project creation failed: Faculty ID {} not found in User Service", facultyId);
			// Throwing this exception triggers the Circuit Breaker

			throw new ResourceNotFoundException("Faculty not found with ID: " + facultyId);
		}

		if (projectRepository.existsByTitleAndFacultyId(project.getTitle(), facultyId)) {
			throw new RuntimeException("A project with this title already exists for this faculty.");
		}

		project.setFacultyId(facultyId);
		project.setStatus(ProjectStatus.DRAFT);

		ResearchProject savedProject = projectRepository.save(project);
		
		// 2. FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(facultyId, "CREATE_PROJECT", "Project Title: " + savedProject.getTitle());

		ResearchProjectDTO responseDTO = modelMapper.map(savedProject, ResearchProjectDTO.class);
		responseDTO.setFaculty(faculty); 
		return responseDTO;
	}

	// ADDED: Fallback Method for createProject
	public ResearchProjectDTO createProjectFallback(ResearchProject project, Long facultyId, Throwable throwable) {
		log.error("Circuit Breaker Tripped! Faculty Service unavailable to verify Faculty {}. Fallback executing. Reason: {}", facultyId, throwable.getMessage());
		
		ResearchProjectDTO fallbackResponse = new ResearchProjectDTO();
		fallbackResponse.setTitle(project.getTitle() + " (CREATION FAILED - SERVICE DOWN)");
		// Returning an empty DTO to prevent server crash, though you could also throw a custom 503 Exception here.
		return fallbackResponse;
	}

	@Override
	public List<ResearchProjectDTO> getProjectsByFaculty(Long facultyId) {
		List<ResearchProject> projects = projectRepository.findByFacultyId(facultyId);

		if (projects.isEmpty()) {
			throw new RuntimeException("No projects found for Faculty ID: " + facultyId);
		}

		// 1. Fetch the Faculty details ONCE before the loop
		FacultyMinimalDTO facultyProfile = null;
		try {
			facultyProfile = facultyClient.getFacultyById(facultyId);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}", facultyId);
		}
		
		// We need a 'final' effectively variable to use inside the lambda stream
		final FacultyMinimalDTO finalFaculty = facultyProfile;

		// 2. Map the projects and attach the faculty profile to each one
		return projects.stream().map(p -> {
			ResearchProjectDTO dto = modelMapper.map(p, ResearchProjectDTO.class);
			dto.setFaculty(finalFaculty); // Attach the fetched data!
			return dto;
		}).collect(java.util.stream.Collectors.toList());
	}

	@Override
	public ResearchProjectDTO getProjectById(Long projectId) {
		ResearchProject project = projectRepository.findById(projectId).orElseThrow(() -> {
			return new RuntimeException("Project not found with ID: " + projectId);
		});

		ResearchProjectDTO responseDTO = modelMapper.map(project, ResearchProjectDTO.class);

		try {
			FacultyMinimalDTO faculty = facultyClient.getFacultyById(project.getFacultyId());
			responseDTO.setFaculty(faculty);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}", project.getFacultyId());
		}

		return responseDTO;
	}

	@Override
	public List<ResearchProject> getProjectsByStatus(ProjectStatus status) {
		return projectRepository.findByStatus(status);
	}

	@Override
	@Transactional
	public ResearchProject updateProjectStatus(Long projectId, ProjectStatus newStatus) {
		log.info("Updating status for Project ID: {} to {}", projectId, newStatus);

		ResearchProject project = projectRepository.findById(projectId).orElseThrow(() -> {
			return new RuntimeException("Project status not updated with ID: " + projectId);
		});

		project.setStatus(newStatus);
		ResearchProject saved = projectRepository.save(project);
		
		// 3. FIRE AUDIT LOG (Fallback to faculty's ID since no user ID was provided in method parameters)
		auditLogger.fireAndForgetLog(project.getFacultyId(), "UPDATE_PROJECT_STATUS", "Project ID: " + projectId + " changed to " + newStatus);
		
		return saved;
	}
	// ResearchProjectServiceImpl.java
	@Override
	public long getTotalCount() {
	    log.info("Fetching total count of research projects");
	    return projectRepository.count();
	}

	@Override
	@Transactional
	public ProjectUpdateResponseDTO updateProject(Long projectId, ResearchProject details) {
		log.info("Processing update request for Project ID: {}", projectId);
 
		ResearchProject existingProject = projectRepository.findById(projectId).orElseThrow(() -> {
			log.error("Update failed: Project ID {} not found", projectId);
			return new ResourceNotFoundException("No project found with ID: " + projectId);
		});
 
		applicationRepository.findByProject_ProjectId(projectId).ifPresent(app -> {
			if (app.getStatus() == GrantApplicationStatus.APPROVED) {
				log.warn("Update blocked: Project ID {} is already APPROVED for a grant", projectId);
				throw new RuntimeException("Update forbidden: This project has been APPROVED for a grant and cannot be modified.");
			}
		});
 
		log.debug("Updating project fields for Project ID: {}", projectId);
		existingProject.setTitle(details.getTitle());
		existingProject.setDescription(details.getDescription());
		existingProject.setStartDate(details.getStartDate());
		existingProject.setEndDate(details.getEndDate());
		existingProject.setStatus(ProjectStatus.DRAFT);
		
		ResearchProject updated = projectRepository.save(existingProject);
		log.info("Project ID: {} updated successfully and set back to DRAFT", projectId);
 
		// 1. Keep your exact original mapping logic
		ProjectUpdateResponseDTO responseDTO = modelMapper.map(updated, ProjectUpdateResponseDTO.class);
 
		// 2. ONLY ADD THIS TRY-CATCH BLOCK
		try {
			FacultyMinimalDTO faculty = facultyClient.getFacultyById(updated.getFacultyId());
			responseDTO.setFaculty(faculty);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}", updated.getFacultyId());
		}
     
		// 3. Return the updated DTO
		return responseDTO;
	}
 
 
}