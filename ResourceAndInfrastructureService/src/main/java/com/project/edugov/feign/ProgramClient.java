package com.project.edugov.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.edugov.dto.ProgramDTO;

@FeignClient(
        name = "program-service",
        path = "/api/programs"
)
public interface ProgramClient {

    @GetMapping("/{programId}")
    ProgramDTO getProgramById(@PathVariable Long programId);
}