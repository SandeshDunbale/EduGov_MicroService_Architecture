package com.project.edugov.dto;

import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;

public record ResourceResponse(
        Long resourceId,
        Long programId,
        ResourceType type,
        Integer quantity,
        ResourceStatus status
) {}