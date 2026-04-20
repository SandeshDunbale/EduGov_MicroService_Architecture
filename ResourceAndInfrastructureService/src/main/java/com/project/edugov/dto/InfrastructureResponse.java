package com.project.edugov.dto;

import com.project.edugov.model.InfrastructureStatus;
import com.project.edugov.model.InfrastructureType;

public record InfrastructureResponse(
        Long infraId,
        Long programId,
        InfrastructureType type,
        String location,
        Integer capacity,
        InfrastructureStatus status
) {}