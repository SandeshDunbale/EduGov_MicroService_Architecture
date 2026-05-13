package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ResearchAndGrantServiceEduGov",contextId = "projectClient")
public interface ProjectClient {
    @GetMapping("/api/projects/count")
    long getTotalProjects();
    
    @GetMapping("/programs/count") 
    long getTotalPrograms();

    @GetMapping("/api/projects/count/status/{status}")
    long getProjectCountByStatus(@PathVariable("status") String status);
}