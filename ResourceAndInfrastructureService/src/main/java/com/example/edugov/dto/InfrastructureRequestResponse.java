package com.example.edugov.dto;

import java.time.Instant;
import com.example.edugov.model.RequestStatus;

public record InfrastructureRequestResponse(
        Long requestId,
        Long requesterUserId,
        String requesterName,
        Long infraId,
        Integer infraCapacity,
        RequestStatus status,
        Long approvedByUserId,
        Instant createdAt,
        Instant updatedAt,
        Instant decisionAt
) {}