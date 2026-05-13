package com.project.edugov.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "academic_documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @Column(nullable = false)
    private Long userId; // Reference to Identity Service

    @Column(nullable = false)
    private String docType; // e.g., TRANSCRIPT, ID_PROOF

    @Column(nullable = false)
    private String docNum;

    @Column(name = "file_url", nullable = false, columnDefinition = "LONGTEXT")
    private String fileUrl;// Path on disk

    @Enumerated(EnumType.STRING)
    private Status verificationStatus = Status.PENDING;

    private Instant uploadedDate = Instant.now();

    private Long verifiedByUserId; // Admin ID from Identity Service
    private Instant verifiedAt;
    
    @Column(length = 1000)
    private String adminNotes;
}