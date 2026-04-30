package com.project.edugov.service;

import java.util.List;

import com.project.edugov.dto.ProjectUpdateResponseDTO;
import com.project.edugov.dto.ResearchProjectDTO;
import com.project.edugov.model.ProjectStatus;
import com.project.edugov.model.ResearchProject;

public interface ResearchProjectService {

	ResearchProjectDTO createProject(ResearchProject project, Long facultyId);

	List<ResearchProjectDTO> getProjectsByFaculty(Long facultyId);

	ResearchProjectDTO getProjectById(Long projectId);

	List<ResearchProject> getProjectsByStatus(ProjectStatus status);

	ResearchProject updateProjectStatus(Long projectId, ProjectStatus newStatus);

	ProjectUpdateResponseDTO updateProject(Long projectId, ResearchProject projectDetails);
}
