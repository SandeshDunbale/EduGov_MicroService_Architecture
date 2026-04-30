package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// The name MUST match 'ACADEMIC-SERVICE' from your properties
@FeignClient(name = "ACADEMICPROGRAMSERVICEEDUGOV")
public interface ProgramClient {

    @GetMapping("/api/programs/count")
    long getTotalPrograms();

    @GetMapping("/api/programs/count/status/{status}")
    long getProgramCountByStatus(@PathVariable("status") String status);
}	