package com.project.edugov.dto;

import lombok.Data;

@Data
public class UserExternalDTO {
    private Long userId;
    private String name;
    
    // to verify if they are a "PROGRAM_MANAGER"
    private String role; 
    private String email;
}