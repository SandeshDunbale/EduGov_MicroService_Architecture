package com.project.edugov.model;
 
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
 
@Entity
@Table(name = "reports")
@Data
public class Report {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;
 
    @Enumerated(EnumType.STRING)
    private ReportScope scope;
 
    @Column(columnDefinition = "TEXT")
    private String metrics;
 
    private LocalDateTime generatedDate;
}
 