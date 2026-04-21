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
	//private final NotificationService notificationService;
	
	// CHANGED: Feign Clients instead of Repositories!
	private final UserClient userClient;
	private final FacultyClient facultyClient;

	@Override
	@Transactional
	public GrantApplicationDTO applyForGrant(GrantApplication newDetails, Long projectId, Long facultyId) {
		log.info("Received grant application request for Project ID: {} from Faculty ID: {}", projectId, facultyId);

		ResearchProject project = projectRepository.findById(projectId).orElseThrow(() -> {
			log.error("Grant application failed: Project ID {} not found", projectId);
			return new ResourceNotFoundException("Project not found with ID: " + projectId);
		});

		Optional<GrantApplication> existingAppOpt = applicationRepository.findByProject_ProjectId(projectId);
		GrantApplication finalApp; 

		if (existingAppOpt.isPresent()) {
			GrantApplication existingApp = existingAppOpt.get();

			if (existingApp.getStatus() != GrantApplicationStatus.REJECTED) {
				log.warn("Duplicate application blocked.");
				throw new RuntimeException("An active application already exists for this project");
			}

			existingApp.setRequestedAmount(newDetails.getRequestedAmount());
			existingApp.setStatus(GrantApplicationStatus.SUBMITTED);
			existingApp.setSubmittedDate(LocalDate.now());

			finalApp = applicationRepository.save(existingApp);
		} else {
			newDetails.setProject(project);
			newDetails.setFacultyId(project.getFacultyId()); // CHANGED: Just the ID
			newDetails.setStatus(GrantApplicationStatus.SUBMITTED);
			newDetails.setSubmittedDate(LocalDate.now());

			finalApp = applicationRepository.save(newDetails);
		}

		// CHANGED: Network call to User Microservice to get all PMs
//		try {
//			List<UserExternalDTO> programManagers = userClient.getUsersByRole("PROG_MANAGER");
//			for (UserExternalDTO pm : programManagers) {
//				notificationService.createNotification(
//						pm.getUserId(),
//						finalApp.getApplicationID(),
//						"New Grant Application submitted for Project: " + project.getTitle(),
//						"GRANTS",
//						pm.getEmail()
//				);
//			}
//		} catch (Exception e) {
//			log.warn("Could not fetch Program Managers from User Service for notifications.");
//		}

		GrantApplicationDTO responseDTO = modelMapper.map(finalApp, GrantApplicationDTO.class);

		// 2. ONLY ADD THIS TRY-CATCH BLOCK
		try {
			// Fetch the rich Faculty details over the network using the saved facultyId
			FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(finalApp.getFacultyId());
			responseDTO.setFaculty(facultyProfile);
		} catch (Exception e) {
			log.warn("Could not fetch Faculty details for ID: {}.", finalApp.getFacultyId());
		}

		// 3. Return the fully populated response
		return responseDTO;
	}

	@Override
	@Transactional
	public GrantResponseDTO approveGrantApplication(Long applicationId, Long userId, GrantStatus decision) {
		log.info("Manager (User ID: {}) is making a decision [{}]", userId, decision);

		GrantApplication app = applicationRepository.findById(applicationId).orElseThrow(() -> {
			return new ResourceNotFoundException("Application not found with ID: " + applicationId);
		});

		// CHANGED: Network call to User Microservice to verify Manager
		UserExternalDTO programManager;
		try {
			programManager = userClient.getUserById(userId);
		} catch (Exception e) {
			throw new ResourceNotFoundException("Manager not found with ID: " + userId + " in User Service");
		}

		ResearchProject project = app.getProject();

		// Fetch the faculty email for notifications using Feign
//		String facultyEmail = null;
//		try {
//			FacultyMinimalDTO facultyDTO = facultyClient.getFacultyById(project.getFacultyId());
//			facultyEmail = facultyDTO.getEmail();
//		} catch (Exception e) {
//			log.warn("Could not fetch Faculty details for notifications.");
//		}

		if (decision == GrantStatus.UNDER_REVIEW) {
			app.setStatus(GrantApplicationStatus.UNDER_REVIEW);
			project.setStatus(ProjectStatus.UNDER_REVIEW);
			projectRepository.save(project);
			applicationRepository.save(app);
			return modelMapper.map(app, GrantResponseDTO.class);
		}

		else if (decision == GrantStatus.APPROVED) {
			app.setStatus(GrantApplicationStatus.APPROVED);
			project.setStatus(ProjectStatus.COMPLETED);
			projectRepository.save(project);

			Grant grant = grantRepository.findByProject_ProjectId(project.getProjectId()).orElse(new Grant());
			grant.setProject(project);
			grant.setFacultyId(project.getFacultyId()); // CHANGED
			grant.setAmount(app.getRequestedAmount());
			grant.setDate(LocalDate.now());
			grant.setStatus(GrantStatus.APPROVED);
			grant.setApprovedByUserId(userId); // CHANGED

			applicationRepository.save(app);
			Grant savedGrant = grantRepository.save(grant);

//			if (facultyEmail != null) {
//				notificationService.createNotification(
//						project.getFacultyId(), app.getApplicationID(), 
//						"Your grant application for '" + project.getTitle() + "' has been APPROVED.", 
//						"GRANTS", facultyEmail
//				);
//			}

			GrantResponseDTO response = modelMapper.map(savedGrant, GrantResponseDTO.class);
			response.setApprovedByRole(programManager.getRole()); // Attach Role from Network call
			return response;
		}

		else if (decision == GrantStatus.REJECTED) {
			app.setStatus(GrantApplicationStatus.REJECTED);
			project.setStatus(ProjectStatus.DRAFT);
			projectRepository.save(project);
			applicationRepository.save(app);

//			if (facultyEmail != null) {
//				notificationService.createNotification(
//						project.getFacultyId(), app.getApplicationID(),
//						"Your grant application for '" + project.getTitle() + "' has been REJECTED.",
//						"GRANTS", facultyEmail
//				);
//			}

			GrantResponseDTO response = modelMapper.map(app, GrantResponseDTO.class);
			response.setApprovedByRole(programManager.getRole()); // Attach Role from Network call
			return response;
		}

		else {
			throw new RuntimeException("Unsupported decision status: " + decision);
		}
	}

//	@Override
//	public List<GrantApplicationDTO> getPendingApplications() {
//		return applicationRepository.findByStatus(GrantApplicationStatus.SUBMITTED).stream()
//				.map(app -> modelMapper.map(app, GrantApplicationDTO.class)).collect(Collectors.toList());
//	}
	@Override
	public List<GrantApplicationDTO> getPendingApplications() {
		return applicationRepository.findByStatus(GrantApplicationStatus.SUBMITTED).stream()
				.map(app -> {
					// 1. Keep your exact original mapping logic for each item
					GrantApplicationDTO dto = modelMapper.map(app, GrantApplicationDTO.class);
					
					// 2. ONLY ADD THIS TRY-CATCH BLOCK
					try {
						FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(app.getFacultyId());
						dto.setFaculty(facultyProfile);
					} catch (Exception e) {
						log.warn("Could not fetch Faculty details for ID: {}. ", app.getFacultyId());
					}
					
					// 3. Return the enriched DTO to the stream
					return dto;
				}).collect(Collectors.toList());
	}

	@Override
	public GrantResponseDTO getGrantByProjectId(Long projectId) {
		Grant grant = grantRepository.findByProject_ProjectId(projectId).orElseThrow(() -> {
			return new RuntimeException("No grant found for this project.");
		});
		
		// 1. Map the basic database fields first (Grant ID, Amount, Date, etc.)
		GrantResponseDTO response = modelMapper.map(grant, GrantResponseDTO.class);
		
		// 2. NEW FIX: Fetch the Manager's role dynamically from the Identity Service!
		if (grant.getApprovedByUserId() != null) {
			try {
				UserExternalDTO manager = userClient.getUserById(grant.getApprovedByUserId());
				response.setApprovedByRole(manager.getRole());
			} catch (Exception e) {
				log.warn("Could not fetch User details for ID: {}", grant.getApprovedByUserId());
				response.setApprovedByRole("UNKNOWN_ROLE"); 
			}
		}

		// 3. Return the fully populated response
		return response;
	}

	@Override
	public List<GrantApplicationDTO> getApplicationHistoryByFaculty(Long facultyId) {
		return applicationRepository.findByFacultyId(facultyId).stream()
				.map(app -> {
					// 1. Keep your exact original mapping logic for each item
					GrantApplicationDTO dto = modelMapper.map(app, GrantApplicationDTO.class);
					
					// 2. ONLY ADD THIS TRY-CATCH BLOCK
					try {
						FacultyMinimalDTO facultyProfile = facultyClient.getFacultyById(app.getFacultyId());
						dto.setFaculty(facultyProfile);
					} catch (Exception e) {
						log.warn("Could not fetch Faculty details for ID: {}.", app.getFacultyId());
					}
					
					// 3. Return the enriched DTO to the stream
					return dto;
				}).collect(Collectors.toList());
	}
}