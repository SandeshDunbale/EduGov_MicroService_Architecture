package com.example.edugov.dto;

import com.example.edugov.model.InfrastructureStatus;
import com.example.edugov.model.InfrastructureType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InfrastructureUpdateRequest(
        @NotNull Long programId,
        @NotNull InfrastructureType type,
        @NotBlank String location,
        @Min(0) Integer capacity,
        @NotNull InfrastructureStatus status
) {}