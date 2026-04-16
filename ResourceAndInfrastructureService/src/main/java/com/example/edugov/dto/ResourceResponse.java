package com.example.edugov.dto;

import com.example.edugov.model.ResourceStatus;
import com.example.edugov.model.ResourceType;

public record ResourceResponse(
        Long resourceId,
        Long programId,
        ResourceType type,
        Integer quantity,
        ResourceStatus status
) {}