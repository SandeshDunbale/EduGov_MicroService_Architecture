package com.example.edugov.dto;

import com.example.edugov.model.InfrastructureStatus;

public record InfrastructureStatusUpdateRequest(
        InfrastructureStatus status
) {}