package com.project.edugov.dto;

import com.project.edugov.model.ResourceStatus;

public record ResourceStatusUpdateRequest(
        ResourceStatus status
) {}