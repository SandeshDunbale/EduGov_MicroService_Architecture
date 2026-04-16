package com.example.edugov.dto;

import java.time.Instant;

import com.example.edugov.model.RequestItemType;
import com.example.edugov.model.RequestStatus;

public record ResourceRequestResponse(
        Long requestId,
        Long requesterUserId,
        Long resourceId,
        Long infrastructureId,
        RequestItemType itemType,
        Integer quantity,
        RequestStatus status,
        Long approvedByUserId,
        Instant createdAt,
        Instant updatedAt,
        Instant decisionAt

) {}