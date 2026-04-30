package com.project.edugov.dto;

import com.project.edugov.model.RequestItemType; // Use your Enum
import com.project.edugov.model.RequestStatus;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceRequestResponse {
    private Long requestId;
    private Long requesterUserId;
    private Long resourceId;
    private RequestItemType itemType; // Changed to your Enum
    private Integer quantity;
    private RequestStatus status;
    private Instant createdAt;
}