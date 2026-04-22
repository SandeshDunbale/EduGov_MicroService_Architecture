package com.project.edugov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    // Account details (Sent to Teammate's IAM)
    private String email;
    private String password;
    
    // Profile details (Saved in your DB)
    private String name;
    private LocalDate dob;
    private String phone;
    private String address;
}