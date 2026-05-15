package com.project.edugov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private Long userId;
    private String userType; // "student" or "faculty"
    private String docType;  // "TRANSCRIPT", "ID_PROOF", etc.
    private String docNum; 
    private String file_url;// The unique ID on the physical document
}
