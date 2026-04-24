package com.project.edugov.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "compliance_record")
public class ComplianceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complianceId;

    @NotNull(message = "Entity ID is required")
    private Long entityId;

    @NotBlank(message = "Entity type (e.g., SCHOOL, NGO) is required")
    private String entityType;

    @Column(nullable = false)
    @NotNull(message = "Compliance result is required")
    private String result = "UNDER_REVIEW";

    @NotNull(message = "Inspection date is required")
    @PastOrPresent(message = "Compliance date cannot be in the future")
    private LocalDate date;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String notes;

    @NotNull(message = "An inspecting officer must be assigned")
    private Long officerId;

    public Long getOfficerId() {
        return officerId;
    }

    public void setOfficerId(Long officerId) {
        this.officerId = officerId;
    }
}
