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

	    // Matches @RequestMapping("/api/grants") + @GetMapping("/applications/status")
	    @GetMapping("/grants/applications/status")
	    List<GrantApplicationDto> getGrantApplicationsByStatus(@RequestParam("status") List<String> statuses);

	    // Matches @RequestMapping("/api/grants") + @GetMapping("/applications/project/{projectId}")
	    @GetMapping("/grants/applications/project/{projectId}")
	    GrantApplicationDto getGrantApplicationByProjectId(@PathVariable("projectId") Long projectId);

	    // Matches @RequestMapping("/api/grants") + @GetMapping("/all")
	    @GetMapping("/grants/all")
	    List<GrantDto> getAllGrants();

	    // This one was already correct!
	    @GetMapping("/grants/project/{projectId}")
	    GrantDto getGrantByProjectId(@PathVariable("projectId") Long projectId);

	    // FIXED: Your ResearchProjectController is mapped to /api/projects, NOT /api/research-projects
	    @GetMapping("/projects/{projectId}")
	    ResearchProjectDto getResearchProjectById(@PathVariable("projectId") Long projectId);
	}