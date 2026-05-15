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
    private final AsyncAuditLogger auditLogger;

    public InfrastructureServiceImpl(
            InfrastructureRepository infraRepo,
            ResourceRequestRepository requestRepo,
            ProgramClient programClient,
            AsyncAuditLogger auditLogger
    ) {
        this.infraRepo = infraRepo;
        this.requestRepo = requestRepo;
        this.programClient = programClient;
        this.auditLogger = auditLogger;
        log.info("✅ InfrastructureServiceImpl initialized");
    }


    //  ADDITION (no behavior change)
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


//    @Override
//
//    public Infrastructure create(
//            Long programId,
//            InfrastructureType type,
//            String location,
//            Integer capacity,
//            InfrastructureStatus status
//    ) {
//        validateProgram(programId);

    public Infrastructure create(Long programId, InfrastructureType type, String location, Integer capacity, InfrastructureStatus status) {
        log.info("Creating Infrastructure → programId={}, type={}, location={}", programId, type, location);

        programClient.getProgramById(programId);
        validateProgram(programId);


        Infrastructure infra = Infrastructure.builder()
                .programId(programId)
                .type(type)
                .location(location)
                .capacity(capacity)
                .status(status)
                .build();

        Infrastructure saved = infraRepo.save(infra);
        
        // Log with 0L representing the System/Admin (until adminId is passed into the method)
        auditLogger.fireAndForgetLog(0L, "CREATE_INFRASTRUCTURE", "Infra ID: " + saved.getInfraId());
        
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Infrastructure getById(Long infraId) {

        return infraRepo.findById(infraId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Infrastructure not found: " + infraId));

        //return infraRepo.findById(infraId).orElseThrow(() -> new EntityNotFoundException("Infrastructure not found: " + infraId));

    }

    @Override
    @Transactional(readOnly = true)
    public List<Infrastructure> findByProgramId(Long programId) {
        return infraRepo.findByProgramId(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Infrastructure> findAll() {
        return infraRepo.findAll();
    }

    @Override
    public Infrastructure updateStatus(Long infraId, InfrastructureStatus status) {
        Infrastructure infra = getById(infraId);
        infra.setStatus(status);
        Infrastructure saved = infraRepo.save(infra);
        
        auditLogger.fireAndForgetLog(0L, "UPDATE_INFRA_STATUS", "Infra ID: " + infraId + " to " + status);
        return saved;
    }

    @Override

//    public Infrastructure update(
//            Long id,
//            Long programId,
//            InfrastructureType type,
//            String location,
//            Integer capacity,
//            InfrastructureStatus status
//    ) {
//        validateProgram(programId);

    public Infrastructure update(Long id, Long programId, InfrastructureType type, String location, Integer capacity, InfrastructureStatus status) {
        programClient.getProgramById(programId);
        validateProgram(programId);


        Infrastructure infra = getById(id);
        infra.setProgramId(programId);
        infra.setType(type);
        infra.setLocation(location);
        infra.setCapacity(capacity);
        infra.setStatus(status);

        Infrastructure saved = infraRepo.save(infra);
        auditLogger.fireAndForgetLog(0L, "UPDATE_INFRASTRUCTURE", "Infra ID: " + id);
        return saved;
    }

    @Override
    public Infrastructure markInUse(Long infraId) {
        Infrastructure infra = getById(infraId);
        if (infra.getStatus() != InfrastructureStatus.AVAILABLE) {
            throw new IllegalStateException("Infrastructure already in use.");
        }

        infra.setStatus(InfrastructureStatus.IN_USE);
        Infrastructure saved = infraRepo.save(infra);
        
        auditLogger.fireAndForgetLog(0L, "MARK_INFRA_IN_USE", "Infra ID: " + infraId);
        return saved;
    }

    @Override
    public void delete(Long infraId) {


        //Infrastructure infra = getById(infraId);

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() -> new EntityNotFoundException("Infrastructure not found: " + infraId));


        long activeRequests = requestRepo.countByInfrastructureAndStatusIn(infra,
                List.of(RequestStatus.SUBMITTED, RequestStatus.IN_REVIEW, RequestStatus.APPROVED));

        if (activeRequests > 0) {
            throw new IllegalStateException("Cannot delete infrastructure " + infraId + " – " + activeRequests + " active requests exist");
        }

        try {
            infraRepo.delete(infra);
            auditLogger.fireAndForgetLog(0L, "DELETE_INFRASTRUCTURE", "Infra ID: " + infraId);
        } catch (DataIntegrityViolationException ex) {

            throw new IllegalStateException(
                    "Cannot delete infrastructure due to related data", ex
            );

            //throw new IllegalStateException("Cannot delete infrastructure due to related data", ex);

        }
    }
}