package com.project.edugov.dto;

import com.project.edugov.model.Status;
import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentResponseDTO {
    private Long studentId; // From your DB
    private Long userId;    // From Teammate's IAM
    private String name;
    private String email;
    private String phone;
    private LocalDate dob;
    private String address;
    private Status status;  // PENDING, APPROVED, or REJECTED
}
