package com.project.edugov.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.client.FacultyClient;
import com.project.edugov.client.NotificationClient;
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

// Resilience4j Import
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

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
	private final AsyncAuditLogger auditLogger;
	private final NotificationClient notificationClient;

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

		// FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(facultyId, "APPLY_GRANT", "Project ID: " + projectId + ", Amount: $" + finalApp.getRequestedAmount());

		// Notify Program Managers via User Service
		try {
			List<UserExternalDTO> programManagers = userClient.getUsersByRole("PROG_MANAGER");
			for (UserExternalDTO pm : programManagers) {
				notificationClient.sendNotification(
						pm.getUserId(),
						finalApp.getApplicationID(),
						"New Grant Application submitted for Project: " + project.getTitle(),
						"GRANTS",
						pm.getEmail()
				);
			}
		} catch (Exception e) {
			log.warn("Could not fetch Program Managers from User Service for notifications.");
		}

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
	@CircuitBreaker(name = "userServiceCb", fallbackMethod = "approveGrantApplicationFallback")
	public GrantResponseDTO approveGrantApplication(Long applicationId, Long userId, GrantStatus decision) {
		log.info("Manager (User ID: {}) is making a decision [{}]", userId, decision);
 
		GrantApplication app = applicationRepository.findById(applicationId).orElseThrow(() -> {
			return new ResourceNotFoundException("Application not found with ID: " + applicationId);
		});
 
		// Network call to User Microservice to verify Manager
		UserExternalDTO programManager;
		try {
			programManager = userClient.getUserById(userId);
		} catch (Exception e) {
			log.error("User Service failed: {}", e.getMessage());
			throw new ResourceNotFoundException("Manager not found with ID: " + userId + " in User Service");
		}
 
		ResearchProject project = app.getProject();
 
		// Fetch the faculty email for notifications using Feign
		String facultyEmail = null;
		try {
			FacultyMinimalDTO facultyDTO = facultyClient.getFacultyById(project.getFacultyId());
			facultyEmail = facultyDTO.getEmail();
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for notifications.");
		}
 
		// ---> ADDED: Track who reviewed the application for the Dashboard <---
		app.setReviewedByUserId(userId);
 
		if (decision == GrantStatus.UNDER_REVIEW) {
			app.setStatus(GrantApplicationStatus.UNDER_REVIEW);
			project.setStatus(ProjectStatus.UNDER_REVIEW);
			projectRepository.save(project);
			applicationRepository.save(app);
			
			GrantResponseDTO response = new GrantResponseDTO();
			response.setProjectId(project.getProjectId());
			response.setProjectTitle(project.getTitle());
			response.setAmount(app.getRequestedAmount());
			response.setDate(LocalDate.now());
			response.setStatus(GrantStatus.UNDER_REVIEW);
			response.setApprovedByRole(programManager.getRole());
			
			return response;
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
 
			if (facultyEmail != null) {
				notificationClient.sendNotification(
						project.getFacultyId(), app.getApplicationID(),
						"Your grant application for '" + project.getTitle() + "' has been APPROVED.",
						"GRANTS", facultyEmail
				);
			}
 
			GrantResponseDTO response = modelMapper.map(savedGrant, GrantResponseDTO.class);
			response.setApprovedByRole(programManager.getRole());
			return response;
		}
 
		else if (decision == GrantStatus.REJECTED) {
			app.setStatus(GrantApplicationStatus.REJECTED);
			project.setStatus(ProjectStatus.DRAFT);
			projectRepository.save(project);
			applicationRepository.save(app);
 
			if (facultyEmail != null) {
				notificationClient.sendNotification(
						project.getFacultyId(), app.getApplicationID(),
						"Your grant application for '" + project.getTitle() + "' has been REJECTED.",
						"GRANTS", facultyEmail
				);
			}
 
			GrantResponseDTO response = new GrantResponseDTO();
			response.setProjectId(project.getProjectId());
			response.setProjectTitle(project.getTitle());
			response.setAmount(app.getRequestedAmount());
			response.setDate(LocalDate.now());
			response.setStatus(GrantStatus.REJECTED);
			response.setApprovedByRole(programManager.getRole());
			
			return response;
		}
 
		else {
			throw new RuntimeException("Unsupported decision status: " + decision);
		}
	}
 
	// --- FALLBACK METHOD FOR APPROVE GRANT ---
	public GrantResponseDTO approveGrantApplicationFallback(Long applicationId, Long userId, GrantStatus decision, Throwable throwable) {
		log.error("Circuit Breaker Tripped! User Service unavailable. Fallback executing. Reason: {}", throwable.getMessage());
		
		GrantResponseDTO fallbackResponse = new GrantResponseDTO();
		fallbackResponse.setStatus(decision); 
		fallbackResponse.setApprovedByRole("SERVICE_UNAVAILABLE");
		
		return fallbackResponse;
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

	// ---> NEW: Method to support the Program Manager Dashboard <---
	@Override
	public List<GrantApplicationDTO> getManagerDecisionHistory(Long managerId) {
		log.info("Fetching grant decision history for Manager ID: {}", managerId);
		
		List<GrantApplicationStatus> statuses = List.of(
			GrantApplicationStatus.APPROVED, 
			GrantApplicationStatus.REJECTED
		);
		
		return applicationRepository.findByReviewedByUserIdAndStatusIn(managerId, statuses).stream()
				.map(app -> {
					GrantApplicationDTO dto = modelMapper.map(app, GrantApplicationDTO.class);
					try {
						FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(app.getFacultyId());
						dto.setFaculty(facultyProfile);
					} catch (Exception e) {
						log.warn("Could not fetch Faculty details for ID: {} in manager history.", app.getFacultyId());
					}
					return dto;
				})
				.collect(Collectors.toList());
	}
}