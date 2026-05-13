package com.project.edugov.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.feign.ProgramClient;
import com.project.edugov.model.RequestStatus;
import com.project.edugov.model.Resource;
import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;
import com.project.edugov.repository.ResourceRepository;
import com.project.edugov.repository.ResourceRequestRepository;

import jakarta.persistence.EntityNotFoundException;

@Slf4j
@Service
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepo;
    private final ResourceRequestRepository requestRepo;
    private final ProgramClient programClient;
    private final AsyncAuditLogger auditLogger;

    public ResourceServiceImpl(
            ResourceRepository resourceRepo,
            ResourceRequestRepository requestRepo,
            ProgramClient programClient,
            AsyncAuditLogger auditLogger
    ) {
        this.resourceRepo = resourceRepo;
        this.requestRepo = requestRepo;
        this.programClient = programClient;
        this.auditLogger = auditLogger;
    }

    @Override
    public Resource create(Long programId, ResourceType type, Integer quantity, ResourceStatus status) {
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
        
        // Log with 0L representing System/Admin
        auditLogger.fireAndForgetLog(0L, "CREATE_RESOURCE", "Resource ID: " + saved.getResourceId());
        return saved;
    }

    @Override
    public Resource getById(Long resourceId) {
        return resourceRepo.findById(resourceId).orElseThrow(() -> new EntityNotFoundException("Resource not found: " + resourceId));
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
        Resource saved = resourceRepo.save(r);
        
        auditLogger.fireAndForgetLog(0L, "UPDATE_RESOURCE_STATUS", "Resource ID: " + resourceId + " to " + status);
        return saved;
    }

    @Override
    public Resource update(Long id, Long programId, ResourceType type, Integer qty, ResourceStatus status) {
        programClient.getProgramById(programId);

        Resource r = getById(id);
        r.setProgramId(programId);
        r.setType(type);
        r.setQuantity(qty);
        r.setStatus(status);

        Resource saved = resourceRepo.save(r);
        auditLogger.fireAndForgetLog(0L, "UPDATE_RESOURCE", "Resource ID: " + id);
        return saved;
    }

    @Override
    public Resource allocate(Long resourceId, int qtyToAllocate) {
        if (qtyToAllocate <= 0) throw new IllegalArgumentException("qtyToAllocate must be > 0");

        Resource r = getById(resourceId);

        if (r.getQuantity() != null) {
            r.setQuantity(r.getQuantity() + qtyToAllocate);
        }

        r.setStatus(ResourceStatus.ALLOCATED);
        Resource saved = resourceRepo.save(r);
        
        auditLogger.fireAndForgetLog(0L, "ALLOCATE_RESOURCE", "Resource ID: " + resourceId + " Qty: " + qtyToAllocate);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return resourceRepo.findAll();
    }

    @Override
    public void delete(Long resourceId) {
        Resource r = getById(resourceId);

        long active = requestRepo.countByResourceAndStatusIn(r,
                List.of(RequestStatus.SUBMITTED, RequestStatus.IN_REVIEW, RequestStatus.APPROVED));

        if (active > 0) {
            throw new IllegalStateException("Cannot delete resource " + resourceId + " – active requests exist");
        }

        try {
            resourceRepo.delete(r);
            auditLogger.fireAndForgetLog(0L, "DELETE_RESOURCE", "Resource ID: " + resourceId);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Cannot delete resource due to related data", ex);
        }
    }
}