package com.project.edugov.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;
    
    // This ID comes from your teammate's IAM Service
    private Long userId; 
    
    private String name;
    private LocalDate dob;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;
}
