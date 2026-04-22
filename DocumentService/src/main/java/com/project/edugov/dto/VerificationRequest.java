package com.project.edugov.dto;

import com.project.edugov.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    private Status status;     // PENDING, APPROVED, or DECLINED
    private String notes;      // Feedback for the user
    private Long adminId;      // ID of the admin performing the action
}
