package com.example.edugov.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitInfrastructureRequest(
        @NotNull Long requesterUserId,
        @NotNull Long infraId
) {}