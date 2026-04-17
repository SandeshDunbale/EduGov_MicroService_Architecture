package com.project.edugov.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProgramServiceImpl implements ProgramService {

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Helper: Combines local Program Entity with remote User Data from Identity Service.
     */
    private ProgramDTO mapToRichDto(Program program) {
        ProgramDTO dto = modelMapper.map(program, ProgramDTO.class);

        try {
            // Fetch remote data using the stored createdByAdminId
            UserFeignDTO admin = userClient.getUserById(program.getCreatedByAdminId());

            if (admin != null) {
                // Mapping from UserFeignDTO to ProgramDTO fields
                dto.setAdminName(admin.getName());
                dto.setAdminEmail(admin.getEmail());
            }
        } catch (Exception e) {
            log.error("Identity Service unavailable. Mapping program ID: {} without admin details.",
                    program.getProgramId());
            dto.setAdminName("Information Currently Unavailable");
            dto.setAdminEmail("N/A");
        }
        return dto;
    }

    @Override
    public ProgramDTO createProgram(Program program, Long adminId) {
        log.info("Attempting to create program: {} by Admin ID: {}", program.getTitle(), adminId);

        // 1. Verify Admin via Feign Client
        UserFeignDTO admin;
        try {
            admin = userClient.getUserById(adminId);
        } catch (Exception e) {
            log.error("Feign call failed: Identity Service is down.");
            throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, "Identity Service is down. Cannot verify Admin.");
        }

        if (admin == null) {
            throw new ResourceNotFoundException("Admin not found with ID: " + adminId);
        }

        // 2. Role Check
        if (!Role.UNIV_ADMIN.equals(admin.getRole())) {
            throw new APIException(HttpStatus.FORBIDDEN, "Access Denied: Only University Admins can create programs.");
        }

        // 3. Duplicate Title Check
        if (programRepo.existsByTitleIgnoreCase(program.getTitle())) {
            throw new APIException(HttpStatus.BAD_REQUEST,
                    "Program title '" + program.getTitle() + "' already exists.");
        }

        // 4. Set the Admin ID and Save
        program.setCreatedByAdminId(adminId);
        Program savedProgram = programRepo.save(program);

        return mapToRichDto(savedProgram);
    }

    @Override
    public ProgramDTO getProgramById(Long id) {
        Program program = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with ID: " + id));
        return mapToRichDto(program);
    }

    @Override
    public List<ProgramDTO> getAllPrograms() {
        List<Program> programs = programRepo.findAll();
        if (programs.isEmpty()) {
            throw new ResourceNotFoundException("No programs are currently registered.");
        }

        return programs.stream()
                .map(this::mapToRichDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProgramDTO> searchPrograms(String title) {
        List<Program> programs = programRepo.findByTitleContainingIgnoreCase(title);
        if (programs.isEmpty()) {
            throw new ResourceNotFoundException("No programs found matching: " + title);
        }

        return programs.stream()
                .map(this::mapToRichDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProgramDTO updateProgramById(Long id, Program details) {
        Program existingProgram = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot update. Program not found with ID: " + id));

        // Update Title with duplicate check
        if (details.getTitle() != null && !details.getTitle().equalsIgnoreCase(existingProgram.getTitle())) {
            if (programRepo.existsByTitleIgnoreCase(details.getTitle())) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Update failed: Title already in use.");
            }
            existingProgram.setTitle(details.getTitle());
        }

        // Update optional fields
        if (details.getDescription() != null) existingProgram.setDescription(details.getDescription());
        if (details.getStartDate() != null) existingProgram.setStartDate(details.getStartDate());
        if (details.getEndDate() != null) existingProgram.setEndDate(details.getEndDate());
        if (details.getStatus() != null) existingProgram.setStatus(details.getStatus());

        Program updatedProgram = programRepo.save(existingProgram);
        return mapToRichDto(updatedProgram);
    }
}