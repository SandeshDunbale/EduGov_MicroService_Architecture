package com.project.edugov.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private String message;
    private Long documentId;
    private String docType;
    private String docNum;
    private String uploadStatus;
    private Instant uploadedAt;
}
