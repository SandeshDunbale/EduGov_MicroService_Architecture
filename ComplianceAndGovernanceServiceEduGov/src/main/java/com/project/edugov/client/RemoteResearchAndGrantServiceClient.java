package com.project.edugov.client;

import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantApplicationDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.ResearchProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "RESEARCHANDGRANTSSERVICEEDUGOV", 
    contextId = "remoteResearchAndGrantServiceClient",
    path = "/api",
    url = "http://localhost:2003" // Direct local connection
)
public interface RemoteResearchAndGrantServiceClient {

    @GetMapping("/grants/applications/status")
    List<GrantApplicationDto> getGrantApplicationsByStatus(@RequestParam("status") List<String> statuses);

    @GetMapping("/grants/project/{projectId}")
    GrantDto getGrantByProjectId(@PathVariable("projectId") Long projectId);

    @GetMapping("/projects/{projectId}")
    ResearchProjectDto getResearchProjectById(@PathVariable("projectId") Long projectId);
}