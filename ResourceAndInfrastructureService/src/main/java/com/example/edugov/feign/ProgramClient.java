package com.example.edugov.feign;

import com.example.edugov.dto.ProgramDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "program-service",
        path = "/api/programs"
)
public interface ProgramClient {

    @GetMapping("/{programId}")
    ProgramDTO getProgramById(@PathVariable Long programId);
}