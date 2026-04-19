package com.project.edugov.model;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "faculty")
public class Faculty {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long facultyId;

    // This is the "soft link" to the Identity Microservice
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private Long userId; 
    
    private String name;
    private String phone;
    private LocalDate dob;
    
    @Column(length = 150)
    private String department;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;
}