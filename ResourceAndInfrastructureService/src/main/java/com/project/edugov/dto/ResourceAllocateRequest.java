package com.project.edugov.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResourceAllocateRequest(
        @NotNull @Min(1) Integer quantity
) {}