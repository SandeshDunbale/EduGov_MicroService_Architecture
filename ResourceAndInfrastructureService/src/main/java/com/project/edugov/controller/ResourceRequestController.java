package com.project.edugov.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.InfrastructureRequestResponse;
import com.project.edugov.dto.ResourceRequestResponse;
import com.project.edugov.dto.SubmitInfrastructureRequest;
import com.project.edugov.dto.SubmitResourceRequest;
import com.project.edugov.model.RequestItemType;
import com.project.edugov.model.RequestStatus;
import com.project.edugov.model.ResourceRequest;
import com.project.edugov.service.ResourceRequestService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/requests")
public class ResourceRequestController {

	private final ResourceRequestService service;
	private ModelMapper mapper;

	public ResourceRequestController(ResourceRequestService service, ModelMapper mapper) {
		this.service = service;
		this.mapper = mapper;
		log.info("✅ ResourceRequestController initialized");
	}

	// =================================================
	// ✅ Submit RESOURCE request
	// =================================================
	@PostMapping("/resource")
	public ResponseEntity<ResourceRequestResponse> submitResource(@Valid @RequestBody SubmitResourceRequest req) {

		log.info("Submit Resource Request → requesterId={}, resourceId={}, qty={}", req.requesterUserId(),
				req.resourceId(), req.quantity());

		ResourceRequest saved = service.submitResourceRequest(req.requesterUserId(), req.resourceId(), req.quantity());

		return ResponseEntity.status(HttpStatus.CREATED).body(mapper.map(saved, ResourceRequestResponse.class));
	}

	// =================================================
	// ✅ Submit INFRASTRUCTURE request
	// =================================================
	@PostMapping("/infrastructure")
	public ResponseEntity<InfrastructureRequestResponse> submitInfrastructure(
			@Valid @RequestBody SubmitInfrastructureRequest req) {

		log.info("Submit Infrastructure Request → requesterId={}, infraId={}", req.requesterUserId(), req.infraId());

		ResourceRequest saved = service.submitInfrastructureRequest(req.requesterUserId(), req.infraId());

		return ResponseEntity.status(HttpStatus.CREATED).body(mapper.map(saved, InfrastructureRequestResponse.class));
	}

	// =================================================
	// ✅ Approve request
	// =================================================
	@PostMapping("/{id}/approve")
	public ResponseEntity<?> approve(@PathVariable Long id, @RequestParam Long approverUserId) {

		ResourceRequest updated = service.approve(id, approverUserId);
		if (updated.getItemType() == RequestItemType.RESOURCE) {
			return ResponseEntity.ok(mapper.map(updated, ResourceRequestResponse.class));
		} else {
			return ResponseEntity.ok(mapper.map(updated, InfrastructureRequestResponse.class));
		}
	}

	// =================================================
	// ✅ Decline request
	// =================================================
	@PostMapping("/{id}/decline")
	public ResponseEntity<?> decline(@PathVariable Long id, @RequestParam Long approverUserId,
			@RequestParam(required = false) String reason) {

		ResourceRequest updated = service.decline(id, approverUserId, reason);

		if (updated.getItemType() == RequestItemType.RESOURCE) {
			return ResponseEntity.ok(mapper.map(updated, ResourceRequestResponse.class));
		} else {
			return ResponseEntity.ok(mapper.map(updated, InfrastructureRequestResponse.class));
		}

	}

	// =================================================
	// ✅ Get by ID
	// =================================================
	@GetMapping("/{id}")
	public ResponseEntity<?> getById(@PathVariable Long id) {
		return ResponseEntity.ok(mapToProperResponse(service.getById(id)));
	}

	// =================================================
	// ✅ List by status
	// =================================================
	@GetMapping
	public List<Object> listByStatus(@RequestParam RequestStatus status) {

		return service.listByStatus(status); // ✅ already mapped
	}

	// =================================================
	// ✅ List by requester
	// =================================================
	@GetMapping("/by-requester/{userId}")
	public List<Object> listByRequester(@PathVariable Long userId) {

		// ✅ Already mapped in service → NO mapping here
		return service.listByRequester(userId);
	}

	// =================================================
	// ✅ MAPPERS
	// =================================================

	private Object mapToProperResponse(ResourceRequest request) {
		return switch (request.getItemType()) {
		case RESOURCE -> mapResourceResponse(request);
		case INFRASTRUCTURE -> mapInfrastructureResponse(request);
		};
	}

	private ResourceRequestResponse mapResourceResponse(ResourceRequest request) {

		return ResourceRequestResponse.builder().requestId(request.getRequestId())
				.requesterUserId(request.getRequesterUserId()).resourceId(request.getResource().getResourceId())
				.resourceType(request.getResource().getType().name()) // ✅ FIX
				.itemType(request.getItemType()).quantity(request.getQuantity()).status(request.getStatus())
				.createdAt(request.getCreatedAt()).reason(request.getReason()).build();
	}

	private InfrastructureRequestResponse mapInfrastructureResponse(ResourceRequest request) {

		return InfrastructureRequestResponse.builder().requestId(request.getRequestId())
				.requesterUserId(request.getRequesterUserId()).infraId(request.getInfrastructure().getInfraId())
				.infrastructureType(request.getInfrastructure().getType().name()) // ✅ FIX
				.itemType(request.getItemType()).status(request.getStatus()).createdAt(request.getCreatedAt())
				.reason(request.getReason()).build();
	}
}
