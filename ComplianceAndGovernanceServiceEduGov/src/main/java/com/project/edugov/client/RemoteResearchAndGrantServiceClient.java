package com.project.edugov.client;

import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantApplicationDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.GrantDto;
import com.project.edugov.dto.RemoteResearchAndGrantDto.ResearchProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "ResearchAndGrantServiceEduGov",
    contextId = "remoteResearchAndGrantServiceClient",
    path = "/api"
)
public interface RemoteResearchAndGrantServiceClient {

    @GetMapping("/grant-applications/status")
    List<GrantApplicationDto> getGrantApplicationsByStatus(@RequestParam("status") List<String> statuses);

    @GetMapping("/grant-applications/project/{projectId}")
    GrantApplicationDto getGrantApplicationByProjectId(@PathVariable("projectId") Long projectId);

    @GetMapping("/grants")
    List<GrantDto> getAllGrants();

    @GetMapping("/grants/project/{projectId}")
    GrantDto getGrantByProjectId(@PathVariable("projectId") Long projectId);

    @GetMapping("/research-projects/{projectId}")
    ResearchProjectDto getResearchProjectById(@PathVariable("projectId") Long projectId);
}
