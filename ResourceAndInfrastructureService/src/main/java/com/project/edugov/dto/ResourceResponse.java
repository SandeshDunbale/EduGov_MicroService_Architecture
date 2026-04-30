package com.project.edugov.dto;

import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;
import lombok.*;

@Data
@NoArgsConstructor // Fixes the 500 Error
@AllArgsConstructor
@Builder
public class ResourceResponse {
    private Long resourceId;
    private Long programId;
    private ResourceType type;
    private Integer quantity;
    private ResourceStatus status;
}