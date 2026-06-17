package com.project.edugov.controller;

import java.util.List;

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
import com.project.edugov.model.Program;
import com.project.edugov.service.ProgramService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/programs")
public class ProgramController {

	private final ProgramService programService;

	// Create a new Program
	@PostMapping("/save")
	public ResponseEntity<ProgramDTO> createProgram(@Valid @RequestBody Program program) {
		log.info("POST: creating new program with title: {}", program.getTitle());
		ProgramDTO result = programService.createProgram(program, program.getCreatedByAdminId());
		log.info("POST: program created successfully with id {}", result.getProgramId());
		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	// Fetch a specific program details by programId
	@GetMapping("/{id}")
	public ResponseEntity<ProgramDTO> getProgramById(@PathVariable Long id) {
		log.info("GET: fetching details for program id: {}", id);
		ProgramDTO result = programService.getProgramById(id);
		log.info("GET: getting program with title : '{}'", result.getTitle());
		return ResponseEntity.ok(result);
	}

	// Fetch a program details by programName
	@GetMapping("/search/{title}")
	public ResponseEntity<List<ProgramDTO>> searchPrograms(@PathVariable String title) {
		log.info("GET: fetching programs matching title: {}", title);
		List<ProgramDTO> results = programService.searchPrograms(title);
		log.info("GET: getting {} programs matching this title", results.size());
		return ResponseEntity.ok(results);
	}

	// Update an existing program
	@PatchMapping("/update/{id}")
	public ResponseEntity<ProgramDTO> updateProgram(@Valid @RequestBody Program details, @PathVariable Long id) {
		log.info("PATCH: updating program {} with new info...", id);
		ProgramDTO result = programService.updateProgramById(id, details);
		log.info("PATCH: program {} updated successfully", result.getProgramId());
		return ResponseEntity.ok(result);
	}

	// List of all programs
	@GetMapping("/all")
	public ResponseEntity<List<ProgramDTO>> getAllPrograms() {
		log.info("GET: fetching full list of programs...");
		List<ProgramDTO> results = programService.getAllPrograms();
		log.info("GET: Total {} programs found", results.size());
		return ResponseEntity.ok(results);
	}

	// Module 6 requirement
	@GetMapping("/status/{status}")
	public ResponseEntity<List<ProgramDTO>> getProgramsByStatus(@PathVariable String status) {
		log.info("API Hit: GET /api/programs/status/{}", status);
		return ResponseEntity.ok(programService.getProgramsByStatus(status));
	}
}