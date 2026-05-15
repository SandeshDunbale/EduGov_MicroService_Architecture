package com.project.edugov.dto;


import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResourceCreateRequest(
        @NotNull Long programId,
        @NotNull ResourceType type,
        @Min(0) Integer quantity,                
        @NotNull ResourceStatus status
) {}