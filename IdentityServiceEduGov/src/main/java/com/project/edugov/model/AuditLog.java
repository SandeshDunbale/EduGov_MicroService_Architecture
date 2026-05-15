package com.project.edugov.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    // RELATIONSHIP: Many audit logs belong to one User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String action; // e.g., "LOGIN", "APPROVE_STUDENT"

    @Column(nullable = false)
    private String resource; // e.g., "USER_MODULE", "GRANT_APPLICATION"

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
}
