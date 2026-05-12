package com.project.edugov.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.NotificationClient;
import com.project.edugov.client.UserClient;
import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.dto.UserFeignDTO;
import com.project.edugov.exception.APIException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Program;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.repository.ProgramRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

	private final ProgramRepository programRepo;
	private final UserClient userClient;
	private final ModelMapper modelMapper;
	private final NotificationClient notificationClient;

	// INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

	// Convert entity to DTO and enrich with Admin data
	private ProgramDTO mapToCustomDto(Program program) {
		ProgramDTO dto = modelMapper.map(program, ProgramDTO.class);
		try {
			// Fetch administrator details from identity service
			UserFeignDTO admin = userClient.getUserById(program.getCreatedByAdminId());
			if (admin != null) {
				dto.setAdminId(admin.getUserId());
				dto.setAdminName(admin.getName());
				dto.setAdminEmail(admin.getEmail());
			} else {
				dto.setAdminId(program.getCreatedByAdminId());
			}
		} catch (Exception e) {
			// Extract root cause for accurate logging
			Throwable root = e;
			while (root.getCause() != null)
				root = root.getCause();
			String errorInfo = root.getMessage() != null ? root.getMessage() : "";

			if (!errorInfo.contains("404") && !errorInfo.contains("NotFound")) {
				log.error("[SYSTEM ERROR] Service connection failed for Admin ID: {}", program.getCreatedByAdminId());
				dto.setAdminName("Information temporarily unavailable");
			} else {
				log.warn("[DATA NOT FOUND] Admin record missing in Identity system for ID: {}",
						program.getCreatedByAdminId());
				dto.setAdminName("Unknown Administrator for the requested record.");
			}
			dto.setAdminId(program.getCreatedByAdminId());
			dto.setAdminEmail("Not available");
		}
		return dto;
	}
	@Override
	public long getTotalCount() {
	    log.info("Fetching total count of academic programs");
	    return programRepo.count();
	}

	@Override
	public ProgramDTO createProgram(Program program, Long adminId) {
		// Log start of creation request
		log.info("[START PROCESS] [POST] Request to /programs/admin/{}", adminId);
		log.info("[ACTION] Validating input for program: {}", program.getTitle());

		// Validate if Admin ID is present
		if (adminId == null || adminId <= 0) {
			log.warn("[VALIDATION FAILED] Admin ID is missing or non-positive");
			throw new APIException(HttpStatus.BAD_REQUEST,
					"A valid Administrator ID is required to perform this action.");
		}

		// Validate start date is not historical
		if (program.getStartDate() != null && program.getStartDate().isBefore(LocalDate.now())) {
			log.warn("[VALIDATION FAILED] Requested start date is in the past");
			throw new APIException(HttpStatus.BAD_REQUEST, "The program start date cannot be in the past.");
		}

		// Validate chronological order of dates
		if (program.getStartDate() != null && program.getEndDate() != null
				&& !program.getEndDate().isAfter(program.getStartDate())) {
			log.warn("[VALIDATION FAILED] End date must follow start date");
			throw new APIException(HttpStatus.BAD_REQUEST,
					"The program end date must be scheduled after the start date.");
		}

		// Verify external user service connectivity
		UserFeignDTO admin = null;
		try {
			admin = userClient.getUserById(adminId);
		} catch (Exception e) {
			Throwable rootCause = e;
			while (rootCause.getCause() != null)
				rootCause = rootCause.getCause();
			String errorMsg = rootCause.getMessage() != null ? rootCause.getMessage() : "";

			if (errorMsg.contains("404") || errorMsg.contains("403") || errorMsg.contains("NotFound")) {
				log.warn("[AUTH FAILED] Admin ID {} not found in system", adminId);
				admin = null;
			} else {
				log.error("[CRITICAL] Identity Service is currently unreachable");
				throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
						"The system is unable to verify administrator credentials. Please contact support.");
			}
		}

		// Verify user has administrative permissions
		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			log.warn("[AUTH FAILED] User ID {} lacks administrative privileges", adminId);
			throw new APIException(HttpStatus.FORBIDDEN,
					"Access Denied: User is not authorized as a University Administrator.");
		}

		// Ensure program title uniqueness
		if (programRepo.existsByTitleIgnoreCase(program.getTitle())) {
			log.warn("[CONFLICT] Program title '{}' is already registered", program.getTitle());
			throw new APIException(HttpStatus.BAD_REQUEST, "The program title provided already exists in the system.");
		}

		// Persistence logic for new program
		program.setCreatedByAdminId(adminId);
		Program savedProgram = programRepo.save(program);
		log.info("[DATABASE SUCCESS] Program record persisted with ID: {}", savedProgram.getProgramId());

		// FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(adminId, "CREATE_PROGRAM", "Program Title: " + savedProgram.getTitle());

		// Broadcast alerts to student population
		try {
			log.info("[NOTIFICATION PROCESS] Initiating student alert broadcast");
			List<UserFeignDTO> students = userClient.getUsersByRole("STUDENT");
			for (UserFeignDTO student : students) {
				try {
					notificationClient.sendNotification(student.getUserId(), savedProgram.getProgramId(),
							"New Program Alert: " + savedProgram.getTitle() + " is now open for enrollment.",
							"PROGRAM_ANNOUNCEMENT", student.getEmail());
				} catch (Exception ex) {
					log.error("[NOTIFICATION FAILED] Delivery failed for Student ID: {}", student.getUserId());
				}
			}
		} catch (Exception ex) {
			log.error("[CRITICAL] Notification service failed during broadcast: {}", ex.getMessage());
		}

		log.info("[SUCCESS] POST request for program creation completed successfully");
		return mapToCustomDto(savedProgram);
	}

	@Override
	public ProgramDTO getProgramById(Long id) {
		// Log specific fetch request
		log.info("[START PROCESS] [GET] Request to /programs/{}", id);

		if (id == null || id <= 0) {
			log.warn("[VALIDATION FAILED] Non-positive ID: {}", id);
			throw new APIException(HttpStatus.BAD_REQUEST, "The provided Program ID is invalid.");
		}

		// Find program or throw custom error
		Program program = programRepo.findById(id).orElseThrow(() -> {
			log.warn("[DATA NOT FOUND] No record exists for Program ID: {}", id);
			return new ResourceNotFoundException("The requested program could not be found in our records.");
		});

		log.info("[SUCCESS] GET request processed for ID: {}", id);
		return mapToCustomDto(program);
	}

	@Override
	public List<ProgramDTO> searchPrograms(String title) {
		// Log search operation
		log.info("[START PROCESS] [GET] Request to /programs/search?title={}", title);

		if (title == null || title.trim().isEmpty()) {
			log.warn("[VALIDATION FAILED] Null or blank search string provided");
			throw new APIException(HttpStatus.BAD_REQUEST, "Please provide a valid search keyword.");
		}

		// Fetch matching program results
		List<Program> programs = programRepo.findByTitleContainingIgnoreCase(title);
		if (programs.isEmpty()) {
			log.warn("[DATA NOT FOUND] No programs match title: {}", title);
			throw new ResourceNotFoundException("No programs match the search criteria provided.");
		}

		log.info("[SUCCESS] Search returned {} matching programs", programs.size());
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public ProgramDTO updateProgramById(Long id, Program details) {
		// Log update request
		log.info("[START PROCESS] [PUT/PATCH] Request to /programs/{}", id);

		// Retrieve existing record from database
		Program existingProgram = programRepo.findById(id).orElseThrow(() -> {
			log.warn("[DATA NOT FOUND] Update target ID {} not found", id);
			return new ResourceNotFoundException("Cannot update: The requested program record was not found.");
		});

		boolean isChanged = false;

		// Conditional logic for updating title
		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existingProgram.getTitle())) {
			if (programRepo.existsByTitleIgnoreCase(details.getTitle())) {
				log.warn("[CONFLICT] Duplicate title during update: {}", details.getTitle());
				throw new APIException(HttpStatus.BAD_REQUEST, "The new title is already assigned to another program.");
			}
			existingProgram.setTitle(details.getTitle());
			isChanged = true;
		}

		// Map description if provided
		if (details.getDescription() != null && !details.getDescription().equals(existingProgram.getDescription())) {
			existingProgram.setDescription(details.getDescription());
			isChanged = true;
		}

		// Update dates if changed
		if (details.getStartDate() != null && !details.getStartDate().equals(existingProgram.getStartDate())) {
			existingProgram.setStartDate(details.getStartDate());
			isChanged = true;
		}
		if (details.getEndDate() != null && !details.getEndDate().equals(existingProgram.getEndDate())) {
			existingProgram.setEndDate(details.getEndDate());
			isChanged = true;
		}

		// Update life-cycle status
		if (details.getStatus() != null && !details.getStatus().equals(existingProgram.getStatus())) {
			existingProgram.setStatus(details.getStatus());
			isChanged = true;
		}

		// Verify if any modifications were submitted
		if (!isChanged) {
			log.warn("[VALIDATION FAILED] Update request contains no new information");
			throw new APIException(HttpStatus.BAD_REQUEST,
					"No changes detected. The program record is already up to date.");
		}

		// Verify date consistency before saving
		if (!existingProgram.getEndDate().isAfter(existingProgram.getStartDate())) {
			log.warn("[VALIDATION FAILED] Final date range is logically inconsistent");
			throw new APIException(HttpStatus.BAD_REQUEST,
					"Validation Error: The end date must occur after the start date.");
		}

		// Save updated record
		Program saved = programRepo.save(existingProgram);
		log.info("[DATABASE SUCCESS] Program ID {} updated in database", saved.getProgramId());

		// FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(saved.getCreatedByAdminId(), "UPDATE_PROGRAM", "Program ID: " + id);

		return mapToCustomDto(saved);
	}

	@Override
	public List<ProgramDTO> getAllPrograms() {
		// Log bulk retrieval
		log.info("[START PROCESS] [GET] Request to /programs/all");

		List<Program> programs = programRepo.findAll();
		if (programs.isEmpty()) {
			log.warn("[DATA NOT FOUND] Database contains no program entries");
			throw new ResourceNotFoundException("There are currently no programs registered in the system.");
		}

		log.info("[SUCCESS] Bulk program retrieval successful");
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<ProgramDTO> getProgramsByStatus(String status) {
		// Log filtered retrieval
		log.info("[START PROCESS] [GET] Request to /programs/status/{}", status);

		try {
			// Convert string input to Enum type
			Status enumStatus = Status.valueOf(status.toUpperCase());
			List<Program> programs = programRepo.findByStatus(enumStatus);

			if (programs.isEmpty()) {
				log.warn("[DATA NOT FOUND] No programs matching status: {}", status);
				throw new ResourceNotFoundException("No programs currently exist with the requested status.");
			}

			log.info("[SUCCESS] Filtered results retrieved for status: {}", status);
			return programs.stream().map(this::mapToCustomDto).collect(Collectors.toList());

		} catch (IllegalArgumentException e) {
			// Handle invalid status types
			log.warn("[VALIDATION FAILED] Client provided invalid status constant: {}", status);
			throw new APIException(HttpStatus.BAD_REQUEST, "The requested status is not recognized by the system.");
		}
	}
}