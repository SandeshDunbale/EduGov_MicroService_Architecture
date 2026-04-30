
package com.project.edugov.dto;

public record DeleteResourceResponse(
        Long resourceId,
        boolean deleted,
        String message
) {}