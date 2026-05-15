package com.project.edugov.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitResourceRequest(
        @NotNull Long requesterUserId,
        @NotNull Long resourceId,
        @NotNull @Min(1) Integer quantity
) {}