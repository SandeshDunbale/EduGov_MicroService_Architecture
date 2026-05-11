package com.project.edugov.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class FacultyDTO {
    private String name;
    private String email;     // For Identity Service
    private String password;  // For Identity Service
    private String phone;
    private LocalDate dob;
    private String address;
    private String department;
}