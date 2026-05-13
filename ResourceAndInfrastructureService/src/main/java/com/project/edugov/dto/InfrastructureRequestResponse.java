package com.project.edugov.dto;

import com.project.edugov.model.RequestItemType; // Use your Enum
import com.project.edugov.model.RequestStatus;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureRequestResponse {
    private Long requestId;
    private Long requesterUserId;
    private Long infraId;           // This matches the .infraId() in config
    private Integer infraCapacity;
    private RequestItemType itemType; // Changed to your Enum
    private RequestStatus status;
    private Long approvedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant decisionAt;
    String reason;
    private String infrastructureType;
    private String programName;
    private String location;
}