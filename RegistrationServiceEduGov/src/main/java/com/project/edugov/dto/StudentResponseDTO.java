package com.project.edugov.dto;

import java.time.LocalDate;

import com.project.edugov.model.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor// Crucial for Jackson to create the object
@AllArgsConstructor
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
