package com.project.edugov.dto;

import com.project.edugov.model.InfrastructureStatus;
import com.project.edugov.model.InfrastructureType;
import lombok.*;

@Data
@NoArgsConstructor // Fixes the 500 Error
@AllArgsConstructor
@Builder
public class InfrastructureResponse {
    private Long infraId;
    private Long programId;
    private InfrastructureType type;
    private String location;
    private Integer capacity;
    private InfrastructureStatus status;
}