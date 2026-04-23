package com.project.edugov.dto;

import java.math.BigDecimal;

public class RemoteResearchAndGrantDto {

    public static class ResearchProjectDto {
        private Long projectId;

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }
    }

    public static class GrantDto {
        private Long grantId;
        private ResearchProjectDto project;
        private BigDecimal amount;

        public Long getGrantId() {
            return grantId;
        }

        public void setGrantId(Long grantId) {
            this.grantId = grantId;
        }

        public ResearchProjectDto getProject() {
            return project;
        }

        public void setProject(ResearchProjectDto project) {
            this.project = project;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class GrantApplicationDto {
        private Long applicationID;
        private ResearchProjectDto project;
        private String status;
        private BigDecimal requestedAmount;

        public Long getApplicationID() {
            return applicationID;
        }

        public void setApplicationID(Long applicationID) {
            this.applicationID = applicationID;
        }

        public ResearchProjectDto getProject() {
            return project;
        }

        public void setProject(ResearchProjectDto project) {
            this.project = project;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getRequestedAmount() {
            return requestedAmount;
        }

        public void setRequestedAmount(BigDecimal requestedAmount) {
            this.requestedAmount = requestedAmount;
        }
    }
}
