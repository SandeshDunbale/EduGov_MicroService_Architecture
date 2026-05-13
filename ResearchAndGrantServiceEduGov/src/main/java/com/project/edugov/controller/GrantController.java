package com.project.edugov.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.GrantApplicationDTO;
import com.project.edugov.dto.GrantResponseDTO;
import com.project.edugov.model.GrantApplication;
import com.project.edugov.model.GrantStatus;
import com.project.edugov.service.GrantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/grants")
@RequiredArgsConstructor
@Slf4j
public class GrantController {

	private final GrantService grantService;
	// private final AuditServiceImpl auditService;

	@PostMapping("/apply/{projectId}")
	public ResponseEntity<GrantApplicationDTO> applyForGrant(@Valid @RequestBody GrantApplication application,
			@PathVariable Long projectId, @RequestParam Long facultyId) {

		// auditService.logAction("APPLY_GRANT", "PROJECT_ID_" + projectId);

		log.info("API Hit: POST /api/grants/apply/{} | Faculty ID: {}", projectId, facultyId);

		GrantApplicationDTO submittedApp = grantService.applyForGrant(application, projectId, facultyId);

		log.info("Successfully processed application. Project: {}, Application ID: {}", projectId,
				submittedApp.getApplicationID());

		return new ResponseEntity<>(submittedApp, HttpStatus.CREATED); // The empty < > is called the Diamond Operator.
																		// Because you already defined the type at the
																		// start of the method (public
																		// ResponseEntity<GrantApplicationDTO>), Java is
																		// smart enough to "infer" (guess) the type. You
																		// don't have to type the long name twice!
	}

	@GetMapping("/pending")
	public ResponseEntity<List<GrantApplicationDTO>> getPendingApplications() {
		log.info("API Hit: GET /api/grants/pending");

		List<GrantApplicationDTO> pending = grantService.getPendingApplications();
		log.debug("Found {} pending applications in system", pending.size());

		return ResponseEntity.ok(pending);
	}

	@PostMapping("/decision/{applicationId}")
	public ResponseEntity<?> approveGrant(@PathVariable Long applicationId, @RequestParam Long userId,
			@RequestParam GrantStatus decision) {

		// auditService.logAction("GRANT_DECISION_" + decision.name(), "APPLICATION_ID_"
		// + applicationId);

		log.info("API Hit: POST /api/grants/decision/{} | Action: {} | Manager ID: {}", applicationId, decision,
				userId);

		GrantResponseDTO result = grantService.approveGrantApplication(applicationId, userId, decision);

		// Rejection
		if (decision == GrantStatus.REJECTED) {
			log.warn("Manager {} REJECTED Application {}. Returning rejection guidance to Faculty.", userId,
					applicationId);

			java.util.Map<String, Object> response = new java.util.HashMap<>();
			response.put("data", result);
			response.put("message",
					"Application Rejected. Your project has been set back to DRAFT. You can now update your project details and re-apply.");
			return ResponseEntity.ok(response);
		}

		log.info("Decision [{}] for Application {} recorded successfully.", decision, applicationId);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/history/{facultyId}")
	public ResponseEntity<List<GrantApplicationDTO>> getHistory(@PathVariable Long facultyId) {
		log.info("API Hit: GET /api/grants/history/{} | Fetching faculty records", facultyId);
		return ResponseEntity.ok(grantService.getApplicationHistoryByFaculty(facultyId));
	}

	@GetMapping("/project/{projectId}")
	public ResponseEntity<GrantResponseDTO> getGrantDetails(@PathVariable Long projectId) {
		log.info("API Hit: GET /api/grants/project/{} | Fetching grant details", projectId);
		return ResponseEntity.ok(grantService.getGrantByProjectId(projectId));
	}

	// Module 6 Requirement
	@GetMapping("/applications/status")
	public ResponseEntity<List<GrantApplicationDTO>> getApplicationsByStatus(
			@RequestParam("status") List<String> statuses) {
		log.info("API Hit: GET /api/grants/applications/status | Statuses: {}", statuses);
		return ResponseEntity.ok(grantService.getGrantApplicationsByStatuses(statuses));
	}

	@GetMapping("/applications/project/{projectId}")
	public ResponseEntity<GrantApplicationDTO> getApplicationByProjectId(@PathVariable Long projectId) {
		log.info("API Hit: GET /api/grants/applications/project/{}", projectId);
		return ResponseEntity.ok(grantService.getGrantApplicationByProjectId(projectId));
	}

	@GetMapping("/all")
	public ResponseEntity<List<GrantResponseDTO>> getAllGrants() {
		log.info("API Hit: GET /api/grants/all");
		return ResponseEntity.ok(grantService.getAllGrants());
	}

	// ---> NEW: Endpoint for the Program Manager Dashboard <---
	@GetMapping("/history/manager/{managerId}")
	public ResponseEntity<List<GrantApplicationDTO>> getManagerDecisionHistory(@PathVariable Long managerId) {
		log.info("API Hit: GET /api/grants/history/manager/{} | Fetching manager decision history", managerId);

		List<GrantApplicationDTO> history = grantService.getManagerDecisionHistory(managerId);
		log.debug("Found {} previous decisions for Manager ID: {}", history.size(), managerId);

		return ResponseEntity.ok(history);
	}
}