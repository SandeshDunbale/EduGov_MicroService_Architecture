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
	public ResearchProjectDTO createProject(ResearchProject project, Long facultyId) {
		log.info("Attempting to create a new project: '{}' for Faculty ID: {}", project.getTitle(), facultyId);

		FacultyMinimalDTO faculty;
		try {
			faculty = facultyClient.getFacultyById(facultyId);
		} catch (Exception e) {
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

	@Override
	public List<ResearchProjectDTO> getProjectsByFaculty(Long facultyId) {
		List<ResearchProject> projects = projectRepository.findByFacultyId(facultyId);

		if (projects.isEmpty()) {
			throw new RuntimeException("No projects found for Faculty ID: " + facultyId);
		}

		return projects.stream().map(project -> modelMapper.map(project, ResearchProjectDTO.class))
				.collect(java.util.stream.Collectors.toList());
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

	@Override
	@Transactional
	public ProjectUpdateResponseDTO updateProject(Long projectId, ResearchProject details) {
		log.info("Processing update request for Project ID: {}", projectId);

		ResearchProject existingProject = projectRepository.findById(projectId).orElseThrow(() -> {
			return new ResourceNotFoundException("No project found with ID: " + projectId);
		});

		applicationRepository.findByProject_ProjectId(projectId).ifPresent(app -> {
			if (app.getStatus() == GrantApplicationStatus.APPROVED) {
				throw new RuntimeException("Update forbidden: This project has been APPROVED for a grant and cannot be modified.");
			}
		});

		existingProject.setTitle(details.getTitle());
		existingProject.setDescription(details.getDescription());
		existingProject.setStartDate(details.getStartDate());
		existingProject.setEndDate(details.getEndDate());
		existingProject.setStatus(ProjectStatus.DRAFT);

		ResearchProject updated = projectRepository.save(existingProject);
		
		// 4. FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(existingProject.getFacultyId(), "UPDATE_PROJECT", "Project ID: " + projectId);

		ProjectUpdateResponseDTO responseDTO = modelMapper.map(updated, ProjectUpdateResponseDTO.class);

		try {
			FacultyMinimalDTO faculty = facultyClient.getFacultyById(updated.getFacultyId());
			responseDTO.setFaculty(faculty);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}", updated.getFacultyId());
		}

		return responseDTO;
	}
}