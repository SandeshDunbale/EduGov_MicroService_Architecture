
package com.example.edugov.dto;

public record DeleteResourceResponse(
        Long resourceId,
        boolean deleted,
        String message
) {}