package com.example.edugov.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.edugov.feign.ProgramClient;
import com.example.edugov.model.RequestStatus;
import com.example.edugov.model.Resource;
import com.example.edugov.model.ResourceStatus;
import com.example.edugov.model.ResourceType;
import com.example.edugov.repository.ResourceRepository;
import com.example.edugov.repository.ResourceRequestRepository;

import jakarta.persistence.EntityNotFoundException;

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

    // ================================
    // CREATE RESOURCE
    // ================================
    @Override
    public Resource create(Long programId, ResourceType type, Integer quantity, ResourceStatus status) {

        // ✅ Validate Program via Feign
        programClient.getProgramById(programId);

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

    // ================================
    // GET BY ID
    // ================================
    @Override
    public Resource getById(Long resourceId) {
        return resourceRepo.findById(resourceId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Resource not found: " + resourceId));
    }

    // ================================
    // FIND BY PROGRAM ID
    // ================================
    @Override
    public List<Resource> findByProgramId(Long programId) {
        return resourceRepo.findByProgramId(programId);
    }

    // ================================
    // FIND BY STATUS
    // ================================
    @Override
    public List<Resource> findByStatus(ResourceStatus status) {
        return resourceRepo.findByStatus(status);
    }

    // ================================
    // UPDATE STATUS
    // ================================
    @Override
    public Resource updateStatus(Long resourceId, ResourceStatus status) {
        Resource r = getById(resourceId);
        r.setStatus(status);
        return resourceRepo.save(r);
    }

    // ================================
    // UPDATE RESOURCE
    // ================================
    @Override
    public Resource update(Long id, Long programId, ResourceType type, Integer qty, ResourceStatus status) {

        // ✅ Validate Program via Feign
        programClient.getProgramById(programId);

        Resource r = getById(id);
        r.setProgramId(programId);
        r.setType(type);
        r.setQuantity(qty);
        r.setStatus(status);

        return resourceRepo.save(r);
    }

    // ================================
    // ALLOCATE
    // ================================
    @Override
    public Resource allocate(Long resourceId, int qtyToAllocate) {

        if (qtyToAllocate <= 0) {
            throw new IllegalArgumentException("qtyToAllocate must be > 0");
        }

        Resource r = getById(resourceId);

        if (r.getQuantity() != null) {
            r.setQuantity(r.getQuantity() + qtyToAllocate);
        }

        r.setStatus(ResourceStatus.ALLOCATED);
        return resourceRepo.save(r);
    }

    // ================================
    // FIND ALL
    // ================================
    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return resourceRepo.findAll();
    }

    // ================================
    // DELETE
    // ================================
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