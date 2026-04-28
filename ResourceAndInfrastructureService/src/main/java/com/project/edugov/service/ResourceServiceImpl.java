package com.project.edugov.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.exception.DownstreamServiceUnavailableException;
import com.project.edugov.feign.ProgramClient;
import com.project.edugov.model.*;
import com.project.edugov.repository.ResourceRepository;
import com.project.edugov.repository.ResourceRequestRepository;

@Slf4j
@Service
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepo;
    private final ResourceRequestRepository requestRepo;
    private final ProgramClient programClient;

    public ResourceServiceImpl(
            ResourceRepository resourceRepo,
            ResourceRequestRepository requestRepo,
            ProgramClient programClient
    ) {
        this.resourceRepo = resourceRepo;
        this.requestRepo = requestRepo;
        this.programClient = programClient;
    }

    // ✅ ADDITION (no behavior change)
    @CircuitBreaker(name = "programService", fallbackMethod = "programFallback")
    private ProgramDTO validateProgram(Long programId) {
        return programClient.getProgramById(programId);
    }

    private ProgramDTO programFallback(Long programId, Throwable ex) {
        log.error("Program service DOWN. programId={}", programId, ex);
        throw new DownstreamServiceUnavailableException(
                "ACADEMICPROGRAMSERVICEEDUGOV",
                "Program service is unavailable. Cannot process resource operation."
        );
    }

    @Override
    public Resource create(Long programId, ResourceType type, Integer quantity, ResourceStatus status) {

        // ✅ SAME CALL – now protected
        validateProgram(programId);

        Resource saved = resourceRepo.save(
                Resource.builder()
                        .programId(programId)
                        .type(type)
                        .quantity(quantity)
                        .status(status)
                        .build()
        );

        log.debug("Resource created → id={}", saved.getResourceId());
        return saved;
    }

    @Override
    public Resource getById(Long resourceId) {
        return resourceRepo.findById(resourceId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Resource not found: " + resourceId));
    }

    @Override
    public List<Resource> findByProgramId(Long programId) {
        return resourceRepo.findByProgramId(programId);
    }

    @Override
    public List<Resource> findByStatus(ResourceStatus status) {
        return resourceRepo.findByStatus(status);
    }

    @Override
    public Resource updateStatus(Long resourceId, ResourceStatus status) {
        Resource r = getById(resourceId);
        r.setStatus(status);
        return resourceRepo.save(r);
    }

    @Override
    public Resource update(Long id, Long programId, ResourceType type, Integer qty, ResourceStatus status) {

        // ✅ SAME CALL – now protected
        validateProgram(programId);

        Resource r = getById(id);
        r.setProgramId(programId);
        r.setType(type);
        r.setQuantity(qty);
        r.setStatus(status);

        return resourceRepo.save(r);
    }

    @Override
    public Resource allocate(Long resourceId, int qtyToAllocate) {

        if (qtyToAllocate <= 0) {
            throw new IllegalArgumentException("qtyToAllocate must be > 0");
        }

        Resource r = getById(resourceId);

        if (r.getQuantity() != null) {
            r.setQuantity(r.getQuantity() - qtyToAllocate);
        }

        r.setStatus(ResourceStatus.AVAILABLE);
        return resourceRepo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return resourceRepo.findAll();
    }

    @Override
    public void delete(Long resourceId) {

        Resource r = getById(resourceId);

        long active = requestRepo.countByResourceAndStatusIn(
                r,
                List.of(
                        RequestStatus.SUBMITTED,
                        RequestStatus.IN_REVIEW,
                        RequestStatus.APPROVED
                )
        );

        if (active > 0) {
            throw new IllegalStateException(
                    "Cannot delete resource " + resourceId + " – active requests exist"
            );
        }

        try {
            resourceRepo.delete(r);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Cannot delete resource due to related data", ex
            );
        }
    }
}