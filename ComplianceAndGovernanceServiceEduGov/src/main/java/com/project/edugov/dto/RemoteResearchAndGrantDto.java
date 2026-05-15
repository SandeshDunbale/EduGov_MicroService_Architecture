package com.project.edugov.dto;

import java.math.BigDecimal;
import lombok.Data;

public class RemoteResearchAndGrantDto {

    @Data
    public static class ResearchProjectDto {
        private Long projectId;
        private String title;
        private String status;
    }

    @Data
    public static class GrantDto {
        private Long grantId;
        private Long projectId;
        private String status;
        private Double amount;
    }

    @Data
    public static class GrantApplicationDto {
        private Long applicationID;
        private ResearchProjectDto project; // Nested object from the Research Service
        private String status;
        private BigDecimal requestedAmount;

        // ✅ FIX: This bridge method resolves the "undefined getProjectId" error
        public Long getProjectId() {
            return (project != null) ? project.getProjectId() : null;
        }
    }
}