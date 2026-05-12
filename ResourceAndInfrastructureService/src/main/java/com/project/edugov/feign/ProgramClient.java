package com.project.edugov.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.ProgramDTO;

@FeignClient(name = "ACADEMICPROGRAMSERVICEEDUGOV") // ✅ MATCHES EUREKA NAME
public interface ProgramClient {
	@GetMapping("/programs/{id}") // Remove /api if the controller doesn't use it
	ProgramDTO getProgramById(@PathVariable("id") Long id);
	
	@GetMapping("/programs/all")
	List<ProgramDTO> getAllPrograms();
}