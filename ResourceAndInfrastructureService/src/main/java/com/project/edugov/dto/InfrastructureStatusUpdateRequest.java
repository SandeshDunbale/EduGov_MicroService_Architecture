package com.project.edugov.dto;

import com.project.edugov.model.InfrastructureStatus;

public record InfrastructureStatusUpdateRequest(
        InfrastructureStatus status
) {}