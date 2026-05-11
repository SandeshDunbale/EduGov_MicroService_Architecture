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
	
	// 1. INJECT AUDIT LOGGER
	private final AsyncAuditLogger auditLogger;

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
			dto.setAdminName("Information temporarily unavailable");
			dto.setAdminId(program.getCreatedByAdminId());
		}
		return dto;
	}

	@Override
	public ProgramDTO createProgram(Program program, Long adminId) {
		if (adminId == null || adminId <= 0) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Valid Admin ID required.");
		}

		if (program.getStartDate() != null && program.getStartDate().isBefore(LocalDate.now())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Start date cannot be in the past.");
		}

		if (program.getStartDate() != null && program.getEndDate() != null
				&& !program.getEndDate().isAfter(program.getStartDate())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "End date must follow start date.");
		}

		UserFeignDTO admin = null;
		try {
			admin = userClient.getUserById(adminId);
		} catch (Exception e) {
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service unreachable.");
		}

		if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
			throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Admin role required.");
		}

		if (programRepo.existsByTitleIgnoreCase(program.getTitle())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Program title already exists.");
		}

		program.setCreatedByAdminId(adminId);
		Program savedProgram = programRepo.save(program);
		
		// 2. FIRE AUDIT LOG
		auditLogger.fireAndForgetLog(adminId, "CREATE_PROGRAM", "Program Title: " + savedProgram.getTitle());

		try {
			List<UserFeignDTO> students = userClient.getUsersByRole("STUDENT");
			for (UserFeignDTO student : students) {
				notificationClient.sendNotification(student.getUserId(), savedProgram.getProgramId(),
						"New Program Alert: " + savedProgram.getTitle(), "PROGRAM_ANNOUNCEMENT", student.getEmail());
			}
		} catch (Exception ex) {
			log.error("Notification broadcast failed.");
		}

		return mapToCustomDto(savedProgram);
	}

	@Override
	public ProgramDTO updateProgramById(Long id, Program details) {
		Program existingProgram = programRepo.findById(id).orElseThrow(() -> 
			new ResourceNotFoundException("Program not found."));

		boolean isChanged = false;

		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existingProgram.getTitle())) {
			if (programRepo.existsByTitleIgnoreCase(details.getTitle())) {
				throw new APIException(HttpStatus.BAD_REQUEST, "Title already assigned.");
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
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected.");
		}

		if (!existingProgram.getEndDate().isAfter(existingProgram.getStartDate())) {
			throw new APIException(HttpStatus.BAD_REQUEST, "End date must occur after start date.");
		}

		Program saved = programRepo.save(existingProgram);
		
		// 3. FIRE AUDIT LOG (Fallback to creator's ID since adminId isn't passed here)
		auditLogger.fireAndForgetLog(saved.getCreatedByAdminId(), "UPDATE_PROGRAM", "Program ID: " + id);
		
		return mapToCustomDto(saved);
	}

	@Override
	public ProgramDTO getProgramById(Long id) {
		Program program = programRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Program missing"));
		return mapToCustomDto(program);
	}

	@Override
	public List<ProgramDTO> searchPrograms(String title) {
		List<Program> programs = programRepo.findByTitleContainingIgnoreCase(title);
		if (programs.isEmpty()) throw new ResourceNotFoundException("No programs match.");
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<ProgramDTO> getAllPrograms() {
		List<Program> programs = programRepo.findAll();
		if (programs.isEmpty()) throw new ResourceNotFoundException("No programs registered.");
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public List<ProgramDTO> getProgramsByStatus(String status) {
		try {
			Status enumStatus = Status.valueOf(status.toUpperCase());
			List<Program> programs = programRepo.findByStatus(enumStatus);
			if (programs.isEmpty()) throw new ResourceNotFoundException("No programs exist with requested status.");
			return programs.stream().map(this::mapToCustomDto).collect(Collectors.toList());
		} catch (IllegalArgumentException e) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Unrecognized status.");
		}
	}
}