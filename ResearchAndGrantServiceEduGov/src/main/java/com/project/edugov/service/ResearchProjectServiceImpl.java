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
	// DELETE THIS
	// private final ModelMapperConfig modelMapper;
	

	// REPLACE IT WITH THIS
	private final ModelMapper modelMapper;
	
	// CHANGED: Injected the Feign Client instead of the Repository
	private final FacultyClient facultyClient;
	
	@Override
	@Transactional
	public ResearchProjectDTO createProject(ResearchProject project, Long facultyId) {
		log.info("Attempting to create a new project: '{}' for Faculty ID: {}", project.getTitle(), facultyId);

		// CHANGED: SYNCHRONOUS NETWORK CALL via Feign
		// If the user service throws a 404, OpenFeign handles it or you can catch it!
		FacultyMinimalDTO faculty;
		try {
			faculty = facultyClient.getFacultyById(facultyId);
		} catch (Exception e) {
			log.error("Project creation failed: Faculty ID {} not found in User Service", facultyId);
			throw new ResourceNotFoundException("Faculty not found with ID: " + facultyId);
		}

		if (projectRepository.existsByTitleAndFacultyId(project.getTitle(), facultyId)) {
			log.warn("Project creation blocked: Title '{}' already exists for Faculty ID {}", project.getTitle(), facultyId);
			throw new RuntimeException("A project with this title already exists for this faculty.");
		}

		// CHANGED: We only store the ID now!
		project.setFacultyId(facultyId);
		project.setStatus(ProjectStatus.DRAFT);

		ResearchProject savedProject = projectRepository.save(project);
		log.info("Project successfully created with ID: {}", savedProject.getProjectId());

		ResearchProjectDTO responseDTO = modelMapper.map(savedProject, ResearchProjectDTO.class);
		responseDTO.setFaculty(faculty); // Attach the network-fetched data to the response
		return responseDTO;
	}

	@Override
	public List<ResearchProjectDTO> getProjectsByFaculty(Long facultyId) {
		log.debug("Fetching all projects for Faculty ID: {}", facultyId);
		List<ResearchProject> projects = projectRepository.findByFacultyId(facultyId);

		if (projects.isEmpty()) {
			log.warn("No projects found in database for Faculty ID: {}", facultyId);
			throw new RuntimeException("No projects found for Faculty ID: " + facultyId);
		}

		return projects.stream().map(project -> modelMapper.map(project, ResearchProjectDTO.class))
				.collect(java.util.stream.Collectors.toList());
	}

	@Override
	public ResearchProjectDTO getProjectById(Long projectId) {
		log.debug("Fetching details for Project ID: {}", projectId);
		ResearchProject project = projectRepository.findById(projectId).orElseThrow(() -> {
			log.error("Fetch failed: Project ID {} not found", projectId);
			return new RuntimeException("Project not found with ID: " + projectId);
		});

		// 1. Keep your exact original mapping logic
		ResearchProjectDTO responseDTO = modelMapper.map(project, ResearchProjectDTO.class);

		// 2. ONLY ADD THIS TRY-CATCH BLOCK
		try {
			FacultyMinimalDTO faculty = facultyClient.getFacultyById(project.getFacultyId());
			responseDTO.setFaculty(faculty);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}", project.getFacultyId());
		}

		// 3. Return the updated DTO
		return responseDTO;
	}

	@Override
	public List<ResearchProject> getProjectsByStatus(ProjectStatus status) {
		log.debug("Fetching projects with status: {}", status);
		return projectRepository.findByStatus(status);
	}

	@Override
	@Transactional
	public ResearchProject updateProjectStatus(Long projectId, ProjectStatus newStatus) {
		log.info("Updating status for Project ID: {} to {}", projectId, newStatus);

		ResearchProject project = projectRepository.findById(projectId).orElseThrow(() -> {
			log.error("Status update failed: Project ID {} not found", projectId);
			return new RuntimeException("Project status not updated with ID: " + projectId);
		});

		project.setStatus(newStatus);
		return projectRepository.save(project);
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

		// ... your existing code above stays exactly the same!
		
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
