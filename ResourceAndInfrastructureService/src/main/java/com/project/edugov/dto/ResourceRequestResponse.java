package com.project.edugov.dto;

import java.time.Instant;

import com.project.edugov.model.RequestItemType;
import com.project.edugov.model.RequestStatus;

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