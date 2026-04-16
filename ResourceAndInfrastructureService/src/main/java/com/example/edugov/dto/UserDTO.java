package com.example.edugov.dto;

public record UserDTO(
        Long userId,
        String name,
        String role,
        boolean active
) {}
