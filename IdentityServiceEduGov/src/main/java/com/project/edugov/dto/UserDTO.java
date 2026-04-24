package com.project.edugov.dto;

public record UserDTO(
        Long userId,
        String name,
        String role,
        boolean active
) {}
