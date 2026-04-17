package com.project.edugov.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "courses")
@Data
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId; // Fixed naming convention (camelCase)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", referencedColumnName = "programId", nullable = false)
    private Program program; // Program is internal to this microservice [cite: 24, 43]

    @NotBlank(message = "Course title is mandatory")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // DECOUPLED: Changed from Faculty object to Long facultyId [cite: 24, 41]
    @Column(name = "faculty_id")
    private Long facultyId; 

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    // DECOUPLED: Changed from User object to Long createdByAdminId [cite: 24, 33]
    @Column(name = "admin_id", updatable = false)
    private Long createdByAdminId;
}