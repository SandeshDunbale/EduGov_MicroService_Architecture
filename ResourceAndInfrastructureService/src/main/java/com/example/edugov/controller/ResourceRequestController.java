package com.example.edugov.controller;

import java.util.List;

import com.example.edugov.dto.*;
import com.example.edugov.model.RequestStatus;
import com.example.edugov.model.ResourceRequest;
import com.example.edugov.service.ResourceRequestService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/requests")
public class ResourceRequestController {

    private final ResourceRequestService service;
    private final ModelMapper mapper;

    public ResourceRequestController(
            ResourceRequestService service,
            ModelMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
        log.info("✅ ResourceRequestController initialized");
    }

    // =================================================
    // Submit RESOURCE request
    // =================================================
    @PostMapping("/resource")
    public ResponseEntity<ResourceRequestResponse> submitResource(
            @Valid @RequestBody SubmitResourceRequest req) {

        log.info("Submit Resource Request → requesterId={}, resourceId={}, qty={}",
                req.requesterUserId(), req.resourceId(), req.quantity());

        ResourceRequest saved = service.submitResourceRequest(
                req.requesterUserId(),
                req.resourceId(),
                req.quantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.map(saved, ResourceRequestResponse.class));
    }

    // =================================================
    // Submit INFRASTRUCTURE request
    // =================================================
    @PostMapping("/infrastructure")
    public ResponseEntity<InfrastructureRequestResponse> submitInfrastructure(
            @Valid @RequestBody SubmitInfrastructureRequest req) {

        log.info("Submit Infrastructure Request → requesterId={}, infraId={}",
                req.requesterUserId(), req.infraId());

        ResourceRequest saved = service.submitInfrastructureRequest(
                req.requesterUserId(),
                req.infraId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.map(saved, InfrastructureRequestResponse.class));
    }

    // =================================================
    // Approve request
    // =================================================
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestParam Long approverUserId) {

        log.info("Approve Request → requestId={}, approverUserId={}", id, approverUserId);

        ResourceRequest updated = service.approve(id, approverUserId);
        return ResponseEntity.ok(mapToProperResponse(updated));
    }

    // =================================================
    // Decline request
    // =================================================
    @PostMapping("/{id}/decline")
    public ResponseEntity<?> decline(
            @PathVariable Long id,
            @RequestParam Long approverUserId,
            @RequestParam(required = false) String reason) {

        log.warn("Decline Request → requestId={}, approverUserId={}, reason={}",
                id, approverUserId, reason);

        ResourceRequest updated = service.decline(id, approverUserId, reason);
        return ResponseEntity.ok(mapToProperResponse(updated));
    }

    // =================================================
    // Get request by ID
    // =================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {

        log.info("Get Request by ID → {}", id);

        ResourceRequest rr = service.getById(id);
        return ResponseEntity.ok(mapToProperResponse(rr));
    }

    // =================================================
    // List requests by status
    // =================================================
    @GetMapping
    public List<?> listByStatus(@RequestParam RequestStatus status) {

        log.info("List Requests by Status → {}", status);

        return service.listByStatus(status)
                .stream()
                .map(this::mapToProperResponse)
                .toList();
    }

    // =================================================
    // List requests by requester
    // =================================================
    @GetMapping("/by-requester/{userId}")
    public List<?> listByRequester(@PathVariable Long userId) {

        log.info("List Requests by Requester → userId={}", userId);

        return service.listByRequester(userId)
                .stream()
                .map(this::mapToProperResponse)
                .toList();
    }

    // =================================================
    // Helper: Map based on request type
    // =================================================
    private Object mapToProperResponse(ResourceRequest request) {

        return switch (request.getItemType()) {
            case RESOURCE ->
                    mapper.map(request, ResourceRequestResponse.class);
            case INFRASTRUCTURE ->
                    mapper.map(request, InfrastructureRequestResponse.class);
        };
    }
}