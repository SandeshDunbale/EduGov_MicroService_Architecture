package com.example.edugov.service;

import java.util.List;

import com.example.edugov.model.Infrastructure;
import com.example.edugov.model.InfrastructureStatus;
import com.example.edugov.model.InfrastructureType;

public interface InfrastructureService {

    Infrastructure create(
            Long programId,
            InfrastructureType type,
            String location,
            Integer capacity,
            InfrastructureStatus status
    );

    Infrastructure getById(Long infraId);

    List<Infrastructure> findByProgramId(Long programId);

    Infrastructure updateStatus(Long infraId, InfrastructureStatus status);

    Infrastructure update(
            Long id,
            Long programId,
            InfrastructureType type,
            String location,
            Integer capacity,
            InfrastructureStatus status
    );

    Infrastructure markInUse(Long infraId);

    List<Infrastructure> findAll();

    void delete(Long infraId);
}