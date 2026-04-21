package com.project.edugov.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.project.edugov.client.UserClient;
import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.dto.UserFeignDTO;
import com.project.edugov.exception.APIException;
import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Program;
import com.project.edugov.model.Role;
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

	// HELPER METHOD
	private ProgramDTO mapToCustomDto(Program program) {
		ProgramDTO dto = modelMapper.map(program, ProgramDTO.class);
		try {
			// Fetching Admin details from Identity Service
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
	public ProgramDTO createProgram(Program program, Long adminId) {
		log.info("Creating new program with title: {}", program.getTitle());
		if (adminId == null || adminId <= 0) {
			log.warn("create failed: admin id is invalid or null");
			throw new APIException(HttpStatus.BAD_REQUEST, "A valid Admin ID is required.");
		}

		// 1. Verify Admin via Identity Service
		try {
			UserFeignDTO admin = userClient.getUserById(adminId);
			if (admin == null || !Role.UNIV_ADMIN.equals(admin.getRole())) {
				log.warn("unauthorized: user {} is not an admin in identity service", adminId);
				throw new APIException(HttpStatus.FORBIDDEN,
						"Access Denied: Only University Admins can create programs.");
			}
		} catch (feign.FeignException e) {
			log.error("error: identity service returned status code {}", e.status());
			if (e.status() == 404 || e.status() == 500 || e.status() == 403
					|| e.contentUTF8().toLowerCase().contains("not found")) {
				log.error("not found: admin {} missing in identity service", adminId);
				throw new ResourceNotFoundException("Admin not found with ID: " + adminId);
			}
			throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service is unreachable.");
		}

		if (programRepo.existsByTitleIgnoreCase(program.getTitle())) {
			log.warn("conflict: program title '{}' already exists in db", program.getTitle());
			throw new APIException(HttpStatus.BAD_REQUEST, "Program title already exists.");
		}

		program.setCreatedByAdminId(adminId);
		Program savedProgram = programRepo.save(program);
		log.info("Program created successfully with id {}", savedProgram.getProgramId());
		return mapToCustomDto(savedProgram);
	}

	@Override
	public ProgramDTO getProgramById(Long id) {
		log.info("Fetching details for program id: {}", id);
		if (id == null || id <= 0) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Invalid Program ID provided.");
		}
		Program program = programRepo.findById(id).orElseThrow(() -> {
			log.warn("not found: program with id {} not in db", id);
			return new ResourceNotFoundException("Program not found with ID: " + id);
		});
		log.info("Getting program with title : '{}'", program.getTitle());
		return mapToCustomDto(program);
	}

	@Override
	public List<ProgramDTO> searchPrograms(String title) {
		log.info("Fetching programs matching keyword: {}", title);
		if (title == null || title.trim().isEmpty()) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Search keyword cannot be empty.");
		}
		List<Program> programs = programRepo.findByTitleContainingIgnoreCase(title);
		if (programs.isEmpty()) {
			log.warn("not found: no programs match the title '{}'", title);
			throw new ResourceNotFoundException("No programs found matching: " + title);
		}
		log.info("Getting {} programs matching this title", programs.size());
		return programs.stream().map(this::mapToCustomDto).toList();
	}

	@Override
	public ProgramDTO updateProgramById(Long id, Program details) {
		log.info("Updating program {} with new info...", id);
		Program existingProgram = programRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + id));

		boolean isChanged = false;
		if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existingProgram.getTitle())) {
			if (programRepo.existsByTitleIgnoreCase(details.getTitle())) {
				log.warn("Update failed: title '{}' already in use", details.getTitle());
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
			log.info("No changes detected for program id: {}", id);
			throw new APIException(HttpStatus.BAD_REQUEST, "No changes detected. Program is already up to date.");
		}

		Program saved = programRepo.save(existingProgram);
		log.info("Program {} updated successfully", saved.getProgramId());
		return mapToCustomDto(saved);
	}

	@Override
	public List<ProgramDTO> getAllPrograms() {
		log.info("Fetching full list of programs...");
		List<Program> programs = programRepo.findAll();
		if (programs.isEmpty()) {
			log.warn("db is empty: no programs registered");
			throw new ResourceNotFoundException("No programs are currently registered.");
		}
		log.info("Total {} programs found", programs.size());
		return programs.stream().map(this::mapToCustomDto).toList();
	}
}