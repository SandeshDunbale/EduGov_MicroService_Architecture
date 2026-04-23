package com.project.edugov.dto;

public record UserDTO(
        Long userId,
        String name,
        String role,
        String status // Maps perfectly to the "status": "ACTIVE" JSON field
) {
    // Helper method so your existing "if (!user.active())" logic still works perfectly!
    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status) || "APPROVE".equalsIgnoreCase(status);
    }
}