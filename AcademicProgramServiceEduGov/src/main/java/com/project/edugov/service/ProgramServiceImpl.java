package com.project.edugov.service;

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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

	// HELPER METHOD
	private ProgramDTO mapToCustomDto(Program program) {
		ProgramDTO dto = modelMapper.map(program, ProgramDTO.class);
		try {
			UserFeignDTO admin = userClient.getUserById(program.getCreatedByAdminId());
			if (admin != null) {
				dto.setAdminId(admin.getUserId());
				dto.setAdminName(admin.getName());
				dto.setAdminEmail(admin.getEmail());
			} else {
				dto.setAdminId(program.getCreatedByAdminId());
			}
		} catch (Exception e) {
			log.error("error: identity service unreachable for admin id {}", program.getCreatedByAdminId());
			dto.setAdminId(program.getCreatedByAdminId());
			dto.setAdminName("Information Unavailable");
			dto.setAdminEmail("N/A");
		}
		return dto;
	}

	@Override
	@CircuitBreaker(name = "identityService", fallbackMethod = "createProgramFallback")
	public ProgramDTO createProgram(Program program, Long adminId) {
		log.info("creating new program with title: {}", program.getTitle());

		if (adminId == null || adminId <= 0) {
			log.warn("create failed: admin id is invalid or null");
			throw new APIException(HttpStatus.BAD_REQUEST, "A valid Admin ID is required.");
		}

		UserFeignDTO admin = userClient.getUserById(adminId);

		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			log.warn("unauthorized: user {} is not an admin in identity service", adminId);
			throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Only University Admins can create programs.");
		}

		if (programRepo.existsByTitleIgnoreCase(program.getTitle())) {
			log.warn("conflict: program title '{}' already exists in db", program.getTitle());
			throw new APIException(HttpStatus.BAD_REQUEST, "Program title already exists.");
		}

		program.setCreatedByAdminId(adminId);
		Program savedProgram = programRepo.save(program);
		log.info("program created successfully with id {}", savedProgram.getProgramId());

		try {
			log.info("ACTION: Notifying all students about new program: {}", savedProgram.getTitle());
			List<UserFeignDTO> students = userClient.getUsersByRole("STUDENT");
			for (UserFeignDTO student : students) {
				try {
					notificationClient.sendNotification(student.getUserId(), savedProgram.getProgramId(),
							"New Program Alert: " + savedProgram.getTitle() + " is now open for enrollment!",
							"PROGRAM_ANNOUNCEMENT", student.getEmail());
				} catch (Exception e) {
					log.error("Failed to send notification to student ID {}: {}", student.getUserId(), e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error("GLOBAL NOTIFICATION ERROR: " + e.getMessage());
		}

		return mapToCustomDto(savedProgram);
	}

	public ProgramDTO createProgramFallback(Program program, Long adminId, Throwable t) {
		log.error("FALLBACK: Identity Service is unreachable for Admin verification. Error: {}", t.getMessage());
		throw new APIException(HttpStatus.SERVICE_UNAVAILABLE,
				"The Identity Service is currently unavailable. Program creation cannot be verified at this time.");
	}

	@Override
	public ProgramDTO getProgramById(Long id) {
		log.info("fetching details for program id: {}", id);
		if (id == null || id <= 0) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Invalid Program ID provided.");
		}
		Program program = programRepo.findById(id).orElseThrow(() -> {
			log.warn("not found: program with id {} not in db", id);
			return new ResourceNotFoundException("Program not found with ID: " + id);
		});
		log.info("getting program with title : '{}'", program.getTitle());
		return mapToCustomDto(program);
	}

	@Override
	public List<ProgramDTO> searchPrograms(String title) {
		log.info("fetching programs matching keyword: {}", title);
		if (title == null || title.trim().isEmpty()) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Search keyword cannot be empty.");
		}
		List<Program> programs = programRepo.findByTitleContainingIgnoreCase(title);
		if (programs.isEmpty()) {
			log.warn("not found: no programs match the title '{}'", title);
			throw new ResourceNotFoundException("No programs found matching: " + title);
		}
		log.info("getting {} programs matching this title", programs.size());
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public ProgramDTO updateProgramById(Long id, Program details) {
		log.info("updating program {} with new info...", id);
		Program existingProgram = programRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + id));

		boolean isChanged = false;
		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existingProgram.getTitle())) {
			if (programRepo.existsByTitleIgnoreCase(details.getTitle())) {
				log.warn("update failed: title '{}' already in use", details.getTitle());
				throw new APIException(HttpStatus.BAD_REQUEST, "Title already in use.");
			}
			existingProgram.setTitle(details.getTitle());
			isChanged = true;
		}
		if (details.getDescription() != null && !details.getDescription().equals(existingProgram.getDescription())) {
			existingProgram.setDescription(details.getDescription());
			isChanged = true;
		}
		if (details.getStartDate() != null && !details.getStartDate().equals(existingProgram.getStartDate())) {
			existingProgram.setStartDate(details.getStartDate());
			isChanged = true;
		}
		if (details.getEndDate() != null && !details.getEndDate().equals(existingProgram.getEndDate())) {
			existingProgram.setEndDate(details.getEndDate());
			isChanged = true;
		}
		if (details.getStatus() != null && !details.getStatus().equals(existingProgram.getStatus())) {
			existingProgram.setStatus(details.getStatus());
			isChanged = true;
		}

		if (!isChanged) {
			log.info("no changes detected for program id: {}", id);
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected. Program is already up to date.");
		}

		Program saved = programRepo.save(existingProgram);
		log.info("program {} updated successfully", saved.getProgramId());
		return mapToCustomDto(saved);
	}

	@Override
	public List<ProgramDTO> getAllPrograms() {
		log.info("fetching full list of programs...");
		List<Program> programs = programRepo.findAll();
		if (programs.isEmpty()) {
			log.warn("db is empty: no programs registered");
			throw new ResourceNotFoundException("No programs are currently registered.");
		}
		log.info("Total {} programs found", programs.size());
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<ProgramDTO> getProgramsByStatus(String status) {
		log.info("fetching programs with status: {}", status);
		Status enumStatus = Status.valueOf(status.toUpperCase());
		List<Program> programs = programRepo.findByStatus(enumStatus);

		if (programs.isEmpty()) {
			log.warn("not found: no programs found with status {}", status);
			throw new ResourceNotFoundException("No programs are currently registered with status: " + status);
		}

		log.info("getting {} programs with status {}", programs.size(), status);
		return programs.stream().map(this::mapToCustomDto).collect(Collectors.toList());
	}
}