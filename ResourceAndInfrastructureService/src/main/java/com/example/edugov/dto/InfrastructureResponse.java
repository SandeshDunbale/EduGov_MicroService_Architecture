package com.example.edugov.dto;

import com.example.edugov.model.InfrastructureStatus;
import com.example.edugov.model.InfrastructureType;

public record InfrastructureResponse(
        Long infraId,
        Long programId,
        InfrastructureType type,
        String location,
        Integer capacity,
        InfrastructureStatus status
) {}