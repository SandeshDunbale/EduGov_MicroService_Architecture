package com.project.edugov.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.exception.DownstreamServiceUnavailableException;
import com.project.edugov.feign.ProgramClient;
import com.project.edugov.model.RequestStatus;
import com.project.edugov.model.Resource;
import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;
import com.project.edugov.repository.ResourceRepository;
import com.project.edugov.repository.ResourceRequestRepository;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

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

    // ✅ Circuit Breaker for Program validation
    @CircuitBreaker(name = "programService", fallbackMethod = "programFallback")
    private ProgramDTO validateProgram(Long programId) {

        try {
            return programClient.getProgramById(programId);
        } catch (FeignException.NotFound ex) {

            throw new EntityNotFoundException("Program ID " + programId + " not found");
        }
    }

    private ProgramDTO programFallback(Long programId, Throwable ex) {
        log.error("Program service DOWN. programId={}", programId, ex);
        throw new DownstreamServiceUnavailableException(
                "ACADEMICPROGRAMSERVICEEDUGOV",
                "Program service is unavailable. Cannot process resource operation."
        );
    }

    // =========================================
    // CREATE
    // =========================================
    @Override
    public Resource create(Long programId, ResourceType type, Integer quantity, ResourceStatus status) {

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

        auditLogger.fireAndForgetLog(
                0L,
                "CREATE_RESOURCE",
                "Resource ID: " + saved.getResourceId()
        );

        return saved;
    }

    // =========================================
    // GET
    // =========================================
    @Override
    public Resource getById(Long resourceId) {
        return resourceRepo.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found: " + resourceId));
    }

    @Override
    public List<Resource> findByProgramId(Long programId) {
        return resourceRepo.findByProgramId(programId);
    }

    @Override
    public List<Resource> findByStatus(ResourceStatus status) {
        return resourceRepo.findByStatus(status);
    }

    // =========================================
    // UPDATE STATUS
    // =========================================
    @Override
    public Resource updateStatus(Long resourceId, ResourceStatus status) {

        Resource r = getById(resourceId);
        r.setStatus(status);

        Resource saved = resourceRepo.save(r);

        auditLogger.fireAndForgetLog(
                0L,
                "UPDATE_RESOURCE_STATUS",
                "Resource ID: " + resourceId + " → " + status
        );

        return saved;
    }

    // =========================================
    // UPDATE FULL
    // =========================================
    @Override
    public Resource update(Long id, Long programId, ResourceType type, Integer qty, ResourceStatus status) {

        validateProgram(programId);

        Resource r = getById(id);

        r.setProgramId(programId);
        r.setType(type);
        r.setQuantity(qty);
        r.setStatus(status);

        Resource saved = resourceRepo.save(r);

        auditLogger.fireAndForgetLog(
                0L,
                "UPDATE_RESOURCE",
                "Resource ID: " + id
        );

        return saved;
    }

    // =========================================
    // ALLOCATE
    // =========================================
    @Override
    public Resource allocate(Long resourceId, int qtyToAllocate) {

        if (qtyToAllocate <= 0) {
            throw new IllegalArgumentException("qtyToAllocate must be > 0");
        }

        Resource r = getById(resourceId);

        if (r.getQuantity() == null || r.getQuantity() < qtyToAllocate) {
            throw new IllegalStateException("Not enough resource quantity available");
        }

        r.setQuantity(r.getQuantity() - qtyToAllocate);
        r.setStatus(ResourceStatus.ALLOCATED);

        Resource saved = resourceRepo.save(r);

        auditLogger.fireAndForgetLog(
                0L,
                "ALLOCATE_RESOURCE",
                "Resource ID: " + resourceId + ", Qty: " + qtyToAllocate
        );

        return saved;
    }

    // =========================================
    // GET ALL
    // =========================================
    @Override
    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return resourceRepo.findAll();
    }

    // =========================================
    // DELETE
    // =========================================
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

            auditLogger.fireAndForgetLog(
                    0L,
                    "DELETE_RESOURCE",
                    "Resource ID: " + resourceId
            );

        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Cannot delete resource due to related data",
                    ex
            );
        }
    }
    public List<Resource> findByTypeAndProgram(Long programId, ResourceType type) {
        return resourceRepo.findByProgramIdAndType(programId, type);
    }
//    public List<ProgramDTO> getAllPrograms() {
//        return programClient.getAllPrograms();
//    }
    @CircuitBreaker(name = "programService", fallbackMethod = "programListFallback")
    public List<ProgramDTO> getAllPrograms() {
        return programClient.getAllPrograms();
    }

    private List<ProgramDTO> programListFallback(Throwable ex) {

        log.warn("Fallback triggered for program list");

        return List.of(
                new ProgramDTO(1L, "BA"),
                new ProgramDTO(2L, "BSc"),
                new ProgramDTO(3L, "B.Tech")
        );
    }


}