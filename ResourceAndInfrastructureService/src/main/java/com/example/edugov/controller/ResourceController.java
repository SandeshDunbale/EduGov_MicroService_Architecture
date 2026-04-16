package com.example.edugov.controller;

import java.util.List;

import com.example.edugov.dto.*;
import com.example.edugov.model.Resource;
import com.example.edugov.service.ResourceService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final ModelMapper mapper;

    public ResourceController(ResourceService resourceService, ModelMapper mapper) {
        this.resourceService = resourceService;
        this.mapper = mapper;
        log.info("✅ ResourceController initialized");
    }

    // =============================================
    // CREATE RESOURCE
    // =============================================
    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceCreateRequest req) {

        log.info("Create Resource → programId={}, type={}, quantity={}",
                req.programId(), req.type(), req.quantity());

        Resource saved = resourceService.create(
                req.programId(),
                req.type(),
                req.quantity(),
                req.status()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.map(saved, ResourceResponse.class));
    }

    // =============================================
    // GET RESOURCE BY ID
    // =============================================
    @GetMapping("/{id}")
    public ResourceResponse get(@PathVariable Long id) {
        log.info("Get Resource by ID → {}", id);
        return mapper.map(resourceService.getById(id), ResourceResponse.class);
    }

    // =============================================
    // UPDATE RESOURCE STATUS
    // =============================================
    @PatchMapping("/{id}/status")
    public ResourceResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ResourceStatusUpdateRequest req) {

        log.info("Update Resource Status → id={}, status={}", id, req.status());

        Resource updated = resourceService.updateStatus(id, req.status());
        return mapper.map(updated, ResourceResponse.class);
    }

    // =============================================
    // UPDATE RESOURCE
    // =============================================
    @PutMapping("/{id}")
    public ResourceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceUpdateRequest req) {

        log.info("Update Resource → id={}", id);

        Resource updated = resourceService.update(
                id,
                req.programId(),
                req.type(),
                req.quantity(),
                req.status()
        );

        return mapper.map(updated, ResourceResponse.class);
    }

    // =============================================
    // ALLOCATE RESOURCE
    // =============================================
    @PostMapping("/{id}/allocate")
    public ResourceResponse allocate(
            @PathVariable Long id,
            @Valid @RequestBody ResourceAllocateRequest req) {

        log.info("Allocate Resource → id={}, quantity={}", id, req.quantity());

        Resource updated = resourceService.allocate(id, req.quantity());
        return mapper.map(updated, ResourceResponse.class);
    }

    // =============================================
    // GET ALL RESOURCES
    // =============================================
    @GetMapping
    public List<ResourceResponse> getAll() {
        log.info("Fetch ALL Resources");

        return resourceService.findAll()
                .stream()
                .map(r -> mapper.map(r, ResourceResponse.class))
                .toList();
    }

    // =============================================
    // DELETE RESOURCE
    // =============================================
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResourceResponse> delete(@PathVariable Long id) {

        log.warn("Delete Resource → id={}", id);
        resourceService.delete(id);

        return ResponseEntity.ok(
                new DeleteResourceResponse(
                        id,
                        true,
                        "Resource deleted successfully"
                )
        );
    }
}