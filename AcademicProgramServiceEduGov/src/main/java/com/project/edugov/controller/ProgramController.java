package com.project.edugov.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.exception.APIException;
import com.project.edugov.model.Program;
import com.project.edugov.service.ProgramService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/programs")
@Slf4j
public class ProgramController {

	@Autowired
	private ProgramService programService;

	/**
	 * Admin Create new program. In Microservices, we expect the adminId to be
	 * passed in the Program object as 'createdByAdminId' or as a separate
	 * header/param.
	 */
	@PostMapping("/save")
	public ResponseEntity<ProgramDTO> createProgram(@Valid @RequestBody Program program) {
		// Accessing the Long ID directly from the Program model
		Long adminId = program.getCreatedByAdminId();

		if (adminId == null) {
			throw new APIException(HttpStatus.BAD_REQUEST, "Admin ID (createdByAdminId) is required.");
		}

		log.info("REST Request: Create program '{}' by Admin ID: {}", program.getTitle(), adminId);
		return new ResponseEntity<>(programService.createProgram(program, adminId), HttpStatus.CREATED);
	}

	// Get all programs
	@GetMapping("/all")
	public ResponseEntity<List<ProgramDTO>> getAllPrograms() {
		log.info("REST Request: Fetching all programs");
		return ResponseEntity.ok(programService.getAllPrograms());
	}

	// Get Program by ID
	@GetMapping("/{id}")
	public ResponseEntity<ProgramDTO> getProgramById(@PathVariable Long id) {
		log.info("REST Request: Fetch Program with ID: {}", id);
		return ResponseEntity.ok(programService.getProgramById(id));
	}

	// Search program by title
	@GetMapping("/search/{title}")
	public ResponseEntity<List<ProgramDTO>> searchPrograms(@PathVariable String title) {
		log.info("REST Request: Search programs by title: {}", title);
		return ResponseEntity.ok(programService.searchPrograms(title));
	}

	// Update the program (Partial update using Patch)
	@PatchMapping("/update/{id}")
	public ResponseEntity<ProgramDTO> updateProgram(@RequestBody Program details, @PathVariable Long id) {
		log.info("REST Request: Update Program with ID: {}", id);
		return ResponseEntity.ok(programService.updateProgramById(id, details));
	}
}