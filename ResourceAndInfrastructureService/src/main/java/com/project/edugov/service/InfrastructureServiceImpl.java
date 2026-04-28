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
import com.project.edugov.repository.InfrastructureRepository;
import com.project.edugov.repository.ResourceRequestRepository;

@Slf4j
@Service
@Transactional
public class InfrastructureServiceImpl implements InfrastructureService {

    private final InfrastructureRepository infraRepo;
    private final ResourceRequestRepository requestRepo;
    private final ProgramClient programClient;

    public InfrastructureServiceImpl(
            InfrastructureRepository infraRepo,
            ResourceRequestRepository requestRepo,
            ProgramClient programClient
    ) {
        this.infraRepo = infraRepo;
        this.requestRepo = requestRepo;
        this.programClient = programClient;
        log.info("✅ InfrastructureServiceImpl initialized");
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
                "Program service is unavailable. Cannot process infrastructure operation."
        );
    }

    // =============================================
    // CREATE
    // =============================================
    @Override
    public Infrastructure create(
            Long programId,
            InfrastructureType type,
            String location,
            Integer capacity,
            InfrastructureStatus status
    ) {
        validateProgram(programId);

        Infrastructure infra = Infrastructure.builder()
                .programId(programId)
                .type(type)
                .location(location)
                .capacity(capacity)
                .status(status)
                .build();

        return infraRepo.save(infra);
    }

    // =============================================
    // GET BY ID
    // =============================================
    @Override
    @Transactional(readOnly = true)
    public Infrastructure getById(Long infraId) {
        return infraRepo.findById(infraId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Infrastructure not found: " + infraId));
    }

    // =============================================
    // FIND BY PROGRAM ID
    // =============================================
    @Override
    @Transactional(readOnly = true)
    public List<Infrastructure> findByProgramId(Long programId) {
        return infraRepo.findByProgramId(programId);
    }

    // =============================================
    // FIND ALL
    // =============================================
    @Override
    @Transactional(readOnly = true)
    public List<Infrastructure> findAll() {
        return infraRepo.findAll();
    }

    // =============================================
    // UPDATE STATUS
    // =============================================
    @Override
    public Infrastructure updateStatus(Long infraId, InfrastructureStatus status) {
        Infrastructure infra = getById(infraId);
        infra.setStatus(status);
        return infraRepo.save(infra);
    }

    // =============================================
    // UPDATE
    // =============================================
    @Override
    public Infrastructure update(
            Long id,
            Long programId,
            InfrastructureType type,
            String location,
            Integer capacity,
            InfrastructureStatus status
    ) {
        validateProgram(programId);

        Infrastructure infra = getById(id);
        infra.setProgramId(programId);
        infra.setType(type);
        infra.setLocation(location);
        infra.setCapacity(capacity);
        infra.setStatus(status);

        return infraRepo.save(infra);
    }

    // =============================================
    // MARK IN USE
    // =============================================
    @Override
    public Infrastructure markInUse(Long infraId) {
        Infrastructure infra = getById(infraId);
        infra.setStatus(InfrastructureStatus.IN_USE);
        return infraRepo.save(infra);
    }

    // =============================================
    // DELETE
    // =============================================
    @Override
    public void delete(Long infraId) {

        Infrastructure infra = getById(infraId);

        long activeRequests = requestRepo.countByInfrastructureAndStatusIn(
                infra,
                List.of(
                        RequestStatus.SUBMITTED,
                        RequestStatus.IN_REVIEW,
                        RequestStatus.APPROVED
                )
        );

        if (activeRequests > 0) {
            throw new IllegalStateException(
                    "Cannot delete infrastructure " + infraId +
                            " – " + activeRequests + " active requests exist"
            );
        }

        try {
            infraRepo.delete(infra);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Cannot delete infrastructure due to related data", ex
            );
        }
    }
}