package com.example.edugov.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.edugov.feign.ProgramClient;
import com.example.edugov.model.*;
import com.example.edugov.repository.InfrastructureRepository;
import com.example.edugov.repository.ResourceRequestRepository;

import jakarta.persistence.EntityNotFoundException;

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

    // =============================================
    // CREATE
    // =============================================
    @Override
    public Infrastructure create(
            Long programId,
            InfrastructureType type,
            String location,
            Integer capacity,
            InfrastructureStatus status) {

        log.info("Creating Infrastructure → programId={}, type={}, location={}",
                programId, type, location);

        // ✅ Validate Program via Feign
        programClient.getProgramById(programId);

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
            InfrastructureStatus status) {

        // ✅ Validate Program via Feign
        programClient.getProgramById(programId);

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

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Infrastructure not found: " + infraId));

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
                    "Cannot delete infrastructure due to related data", ex);
        }
    }
}
