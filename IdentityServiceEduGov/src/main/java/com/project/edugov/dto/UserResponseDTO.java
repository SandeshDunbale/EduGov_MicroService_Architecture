package com.project.edugov.dto;

import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private Status status;
}