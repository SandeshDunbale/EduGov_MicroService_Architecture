package com.project.edugov.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.edugov.dto.InfrastructureCreateRequest;
import com.project.edugov.dto.InfrastructureResponse;
import com.project.edugov.dto.InfrastructureStatusUpdateRequest;
import com.project.edugov.dto.InfrastructureUpdateRequest;
import com.project.edugov.model.Infrastructure;
import com.project.edugov.service.InfrastructureService;

@Slf4j
@RestController
@RequestMapping("/api/infrastructure")
public class InfrastructureController {

    private final InfrastructureService infraService;
    private final ModelMapper mapper;

    public InfrastructureController(
            InfrastructureService infraService,
            ModelMapper mapper
    ) {
        this.infraService = infraService;
        this.mapper = mapper;
        log.info("✅ InfrastructureController initialized");
    }

    // =============================================
    // CREATE INFRASTRUCTURE
    // =============================================
    @PostMapping
    public ResponseEntity<InfrastructureResponse> create(
            @Valid @RequestBody InfrastructureCreateRequest req) {

        log.info("Create Infrastructure → programId={}, type={}, location={}",
                req.programId(), req.type(), req.location());

        Infrastructure saved = infraService.create(
                req.programId(),
                req.type(),
                req.location(),
                req.capacity(),
                req.status()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.map(saved, InfrastructureResponse.class));
    }

    // =============================================
    // GET INFRASTRUCTURE BY ID
    // =============================================
    @GetMapping("/{id}")
    public InfrastructureResponse get(@PathVariable Long id) {

        log.info("Get Infrastructure by ID → {}", id);

        Infrastructure infra = infraService.getById(id);
        return mapper.map(infra, InfrastructureResponse.class);
    }

    // =============================================
    // GET ALL INFRASTRUCTURE
    // =============================================
    @GetMapping("/all")
    public List<InfrastructureResponse> getAll() {

        log.info("Fetch ALL Infrastructure");

        return infraService.findAll()
                .stream()
                .map(i -> mapper.map(i, InfrastructureResponse.class))
                .toList();
    }

    // =============================================
    // UPDATE INFRASTRUCTURE STATUS
    // =============================================
    @PatchMapping("/{id}/status")
    public InfrastructureResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody InfrastructureStatusUpdateRequest req) {

        log.info("Update Infrastructure Status → id={}, status={}",
                id, req.status());

        Infrastructure updated = infraService.updateStatus(id, req.status());
        return mapper.map(updated, InfrastructureResponse.class);
    }

    // =============================================
    // UPDATE INFRASTRUCTURE
    // =============================================
    @PutMapping("/{id}")
    public InfrastructureResponse update(
            @PathVariable Long id,
            @Valid @RequestBody InfrastructureUpdateRequest req) {

        log.info("Update Infrastructure → id={}", id);

        Infrastructure updated = infraService.update(
                id,
                req.programId(),
                req.type(),
                req.location(),
                req.capacity(),
                req.status()
        );

        return mapper.map(updated, InfrastructureResponse.class);
    }

    // =============================================
    // DELETE INFRASTRUCTURE
    // =============================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {

        log.warn("Delete Infrastructure → id={}", id);

        infraService.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Infrastructure deleted successfully")
        );
    }
}