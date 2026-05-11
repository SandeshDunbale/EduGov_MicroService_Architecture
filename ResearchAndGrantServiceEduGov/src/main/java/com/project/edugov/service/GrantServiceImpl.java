package com.project.edugov.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.FacultyClient;
import com.project.edugov.client.UserClient;
import com.project.edugov.dto.FacultyMinimalDTO;
import com.project.edugov.dto.GrantApplicationDTO;
import com.project.edugov.dto.GrantResponseDTO;
import com.project.edugov.dto.UserExternalDTO;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Grant;
import com.project.edugov.model.GrantApplication;
import com.project.edugov.model.GrantApplicationStatus;
import com.project.edugov.model.GrantStatus;
import com.project.edugov.model.ProjectStatus;
import com.project.edugov.model.ResearchProject;
import com.project.edugov.repository.GrantApplicationRepository;
import com.project.edugov.repository.GrantRepository;
import com.project.edugov.repository.ResearchProjectRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrantServiceImpl implements GrantService {

	private final GrantApplicationRepository applicationRepository;
	private final GrantRepository grantRepository;
	private final ResearchProjectRepository projectRepository;
	private final ModelMapper modelMapper;
	
	private final UserClient userClient;
	private final FacultyClient facultyClient;
	
	// 1. INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

	@Override
	@Transactional
	public GrantApplicationDTO applyForGrant(GrantApplication newDetails, Long projectId, Long facultyId) {
		log.info("Received grant application request for Project ID: {} from Faculty ID: {}", projectId, facultyId);

		ResearchProject project = projectRepository.findById(projectId).orElseThrow(() -> {
			return new ResourceNotFoundException("Project not found with ID: " + projectId);
		});

		Optional<GrantApplication> existingAppOpt = applicationRepository.findByProject_ProjectId(projectId);
		GrantApplication finalApp; 

		if (existingAppOpt.isPresent()) {
			GrantApplication existingApp = existingAppOpt.get();

			if (existingApp.getStatus() != GrantApplicationStatus.REJECTED) {
				throw new RuntimeException("An active application already exists for this project");
			}

			existingApp.setRequestedAmount(newDetails.getRequestedAmount());
			existingApp.setStatus(GrantApplicationStatus.SUBMITTED);
			existingApp.setSubmittedDate(LocalDate.now());

			finalApp = applicationRepository.save(existingApp);
		} else {
			newDetails.setProject(project);
			newDetails.setFacultyId(project.getFacultyId());
			newDetails.setStatus(GrantApplicationStatus.SUBMITTED);
			newDetails.setSubmittedDate(LocalDate.now());

			finalApp = applicationRepository.save(newDetails);
		}

		// 2. FIRE AUDIT LOG (Logged under Faculty ID)
		auditLogger.fireAndForgetLog(facultyId, "APPLY_GRANT", "Project ID: " + projectId + ", Amount: $" + finalApp.getRequestedAmount());

		GrantApplicationDTO responseDTO = modelMapper.map(finalApp, GrantApplicationDTO.class);

		try {
			FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(finalApp.getFacultyId());
			responseDTO.setFaculty(facultyProfile);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}.", finalApp.getFacultyId());
		}

		return responseDTO;
	}

	@Override
	@Transactional
	public GrantResponseDTO approveGrantApplication(Long applicationId, Long userId, GrantStatus decision) {
		log.info("Manager (User ID: {}) is making a decision [{}]", userId, decision);

		GrantApplication app = applicationRepository.findById(applicationId).orElseThrow(() -> {
			return new ResourceNotFoundException("Application not found with ID: " + applicationId);
		});

		UserExternalDTO programManager;
		try {
			programManager = userClient.getUserById(userId);
		} catch (Exception e) {
			throw new ResourceNotFoundException("Manager not found with ID: " + userId + " in User Service");
		}

		ResearchProject project = app.getProject();

		if (decision == GrantStatus.UNDER_REVIEW) {
			app.setStatus(GrantApplicationStatus.UNDER_REVIEW);
			project.setStatus(ProjectStatus.UNDER_REVIEW);
			projectRepository.save(project);
			applicationRepository.save(app);
			
			// 3. FIRE AUDIT LOG (Logged under Manager ID)
			auditLogger.fireAndForgetLog(userId, "GRANT_UNDER_REVIEW", "Application ID: " + applicationId);
			
			return modelMapper.map(app, GrantResponseDTO.class);
		}

		else if (decision == GrantStatus.APPROVED) {
			app.setStatus(GrantApplicationStatus.APPROVED);
			project.setStatus(ProjectStatus.COMPLETED);
			projectRepository.save(project);

			Grant grant = grantRepository.findByProject_ProjectId(project.getProjectId()).orElse(new Grant());
			grant.setProject(project);
			grant.setFacultyId(project.getFacultyId()); 
			grant.setAmount(app.getRequestedAmount());
			grant.setDate(LocalDate.now());
			grant.setStatus(GrantStatus.APPROVED);
			grant.setApprovedByUserId(userId); 

			applicationRepository.save(app);
			Grant savedGrant = grantRepository.save(grant);

			// 3. FIRE AUDIT LOG (Logged under Manager ID)
			auditLogger.fireAndForgetLog(userId, "GRANT_APPROVED", "Application ID: " + applicationId + ", Amount: $" + savedGrant.getAmount());

			GrantResponseDTO response = modelMapper.map(savedGrant, GrantResponseDTO.class);
			response.setApprovedByRole(programManager.getRole()); 
			return response;
		}

		else if (decision == GrantStatus.REJECTED) {
			app.setStatus(GrantApplicationStatus.REJECTED);
			project.setStatus(ProjectStatus.DRAFT);
			projectRepository.save(project);
			applicationRepository.save(app);

			// 3. FIRE AUDIT LOG (Logged under Manager ID)
			auditLogger.fireAndForgetLog(userId, "GRANT_REJECTED", "Application ID: " + applicationId);

			GrantResponseDTO response = modelMapper.map(app, GrantResponseDTO.class);
			response.setApprovedByRole(programManager.getRole()); 
			return response;
		}

		else {
			throw new RuntimeException("Unsupported decision status: " + decision);
		}
	}

	@Override
	public List<GrantApplicationDTO> getPendingApplications() {
		return applicationRepository.findByStatus(GrantApplicationStatus.SUBMITTED).stream()
				.map(app -> {
					GrantApplicationDTO dto = modelMapper.map(app, GrantApplicationDTO.class);
					try {
						FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(app.getFacultyId());
						dto.setFaculty(facultyProfile);
					} catch (Exception e) {
						log.warn("Could not fetch Faculty details for ID: {}. ", app.getFacultyId());
					}
					return dto;
				}).collect(Collectors.toList());
	}

	@Override
	public GrantResponseDTO getGrantByProjectId(Long projectId) {
		Grant grant = grantRepository.findByProject_ProjectId(projectId).orElseThrow(() -> {
			return new RuntimeException("No grant found for this project.");
		});
		
		GrantResponseDTO response = modelMapper.map(grant, GrantResponseDTO.class);
		
		if (grant.getApprovedByUserId() != null) {
			try {
				UserExternalDTO manager = userClient.getUserById(grant.getApprovedByUserId());
				response.setApprovedByRole(manager.getRole());
			} catch (Exception e) {
				log.warn("Could not fetch User details for ID: {}", grant.getApprovedByUserId());
				response.setApprovedByRole("UNKNOWN_ROLE"); 
			}
		}

		return response;
	}

	@Override
	public List<GrantApplicationDTO> getApplicationHistoryByFaculty(Long facultyId) {
		return applicationRepository.findByFacultyId(facultyId).stream()
				.map(app -> {
					GrantApplicationDTO dto = modelMapper.map(app, GrantApplicationDTO.class);
					try {
						FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(app.getFacultyId());
						dto.setFaculty(facultyProfile);
					} catch (Exception e) {
						log.warn("Could not fetch Faculty details for ID: {}.", app.getFacultyId());
					}
					return dto;
				}).collect(Collectors.toList());
	}
	
	@Override
	public List<GrantApplicationDTO> getGrantApplicationsByStatuses(List<String> statuses) {
		List<GrantApplicationStatus> enumStatuses = statuses.stream()
				.map(GrantApplicationStatus::valueOf)
				.collect(Collectors.toList());

		return applicationRepository.findByStatusIn(enumStatuses).stream()
				.map(app -> modelMapper.map(app, GrantApplicationDTO.class))
				.collect(Collectors.toList());
	}

	@Override
	public GrantApplicationDTO getGrantApplicationByProjectId(Long projectId) {
		GrantApplication app = applicationRepository.findByProject_ProjectId(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Application not found for project: " + projectId));
		return modelMapper.map(app, GrantApplicationDTO.class);
	}

	@Override
	public List<GrantResponseDTO> getAllGrants() {
		return grantRepository.findAll().stream()
				.map(grant -> modelMapper.map(grant, GrantResponseDTO.class))
				.collect(Collectors.toList());
	}
}