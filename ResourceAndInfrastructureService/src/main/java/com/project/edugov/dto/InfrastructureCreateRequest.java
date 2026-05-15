package com.project.edugov.dto;

import com.project.edugov.model.InfrastructureStatus;
import com.project.edugov.model.InfrastructureType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InfrastructureCreateRequest(
        @NotNull Long programId,
        @NotNull InfrastructureType type,
        @NotBlank String location,
        @Min(0) Integer capacity,
        @NotNull InfrastructureStatus status
) {}