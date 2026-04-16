package com.project.edugov.service;

import java.util.List;

import com.project.edugov.dto.GrantApplicationDTO;
import com.project.edugov.dto.GrantResponseDTO;
import com.project.edugov.model.GrantApplication;
import com.project.edugov.model.GrantStatus;

public interface GrantService {

	GrantApplicationDTO applyForGrant(GrantApplication application, Long projectId, Long facultyId);

	GrantResponseDTO approveGrantApplication(Long applicationId, Long managerId, GrantStatus decision);

	List<GrantApplicationDTO> getPendingApplications();

	GrantResponseDTO getGrantByProjectId(Long projectId);

	List<GrantApplicationDTO> getApplicationHistoryByFaculty(Long facultyId);
}
