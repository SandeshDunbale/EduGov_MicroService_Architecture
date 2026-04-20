package com.project.edugov.dto;

import java.time.LocalDate;
import com.project.edugov.model.Status;
import lombok.Data;

@Data
public class FacultyResponseDTO {
    private Long facultyId;
    private String userId;
    private String name;
    private String email; 
    private String phone;
    private LocalDate dob;
    private String department;
    private Status status;
}
