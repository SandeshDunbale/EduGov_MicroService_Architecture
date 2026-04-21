package com.project.edugov.service;

import java.util.List;

import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.model.Program;

public interface ProgramService {
	ProgramDTO createProgram(Program program, Long adminId);

	List<ProgramDTO> searchPrograms(String title);

	ProgramDTO getProgramById(Long id);

	ProgramDTO updateProgramById(Long id, Program details);

	List<ProgramDTO> getAllPrograms();
}