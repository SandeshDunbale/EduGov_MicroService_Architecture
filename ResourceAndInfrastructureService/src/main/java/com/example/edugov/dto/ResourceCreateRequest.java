package com.example.edugov.dto;


import com.example.edugov.model.ResourceStatus;
import com.example.edugov.model.ResourceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResourceCreateRequest(
        @NotNull Long programId,
        @NotNull ResourceType type,
        @Min(0) Integer quantity,                
        @NotNull ResourceStatus status
) {}