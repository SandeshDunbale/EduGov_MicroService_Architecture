package com.project.edugov.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resourcerequest")
@Data
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Long requestId;

    /** External: User Service */
    @Column(name = "RequesterUserID", nullable = false)
    private Long requesterUserId;

    @ManyToOne(fetch = FetchType.EAGER) // Change from LAZY to EAGER
    @JoinColumn(name = "ResourceID")
    private Resource resource;

    @ManyToOne(fetch = FetchType.EAGER) // Change from LAZY to EAGER
    @JoinColumn(name = "InfraID")
    private Infrastructure infrastructure;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 16, nullable = false)
    private RequestItemType itemType;

    @Column(name = "quantity")
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private RequestStatus status;

    /** External: User Service */
    @Column(name = "ApprovedBy")
    private Long approvedByUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "decision_at")
    private Instant decisionAt;
 // In ResourceRequest model
    @Column(name = "reason")
    private String reason;


    @PrePersist
    @PreUpdate
    private void validatePolymorphicTarget() {

        boolean hasResource = (resource != null);
        boolean hasInfra = (infrastructure != null);

        if (itemType == null) {
            throw new IllegalStateException("itemType is required");
        }

        if (hasResource == hasInfra) {
            throw new IllegalStateException(
                "Exactly one of Resource or Infrastructure must be set");
        }

        if (itemType == RequestItemType.RESOURCE && !hasResource) {
            throw new IllegalStateException(
                "itemType=RESOURCE requires Resource");
        }

        if (itemType == RequestItemType.INFRASTRUCTURE && !hasInfra) {
            throw new IllegalStateException(
                "itemType=INFRASTRUCTURE requires Infrastructure");
        }

        if (itemType == RequestItemType.INFRASTRUCTURE && quantity != null) {
            throw new IllegalStateException(
                "quantity must be null for infrastructure requests");
        }

        if (itemType == RequestItemType.RESOURCE &&
            (quantity == null || quantity <= 0)) {
            throw new IllegalStateException(
                "quantity must be > 0 for resource requests");
        }
    }
}