package com.example.edugov.dto;

import com.example.edugov.model.ResourceStatus;

public record ResourceStatusUpdateRequest(
        ResourceStatus status
) {}