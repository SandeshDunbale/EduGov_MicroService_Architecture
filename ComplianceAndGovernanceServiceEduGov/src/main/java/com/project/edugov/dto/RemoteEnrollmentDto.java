package com.project.edugov.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoteEnrollmentDto {
    private Long enrollmentId;
    private Long studentId;
    private Long courseId; // This connects the student to the course
    private String status;
    private LocalDateTime enrollmentDate;
    private Long approvedByAdminId;
}