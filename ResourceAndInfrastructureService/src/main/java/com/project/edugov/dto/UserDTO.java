package com.project.edugov.dto;

public record UserDTO(
        Long userId,
        String name,
        String role,
        String status,
        String email
) {
    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status)
            || "APPROVE".equalsIgnoreCase(status);
    }
}