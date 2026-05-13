package com.project.edugov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoteCourseDto {
    private Long courseId;
    private String title;
    private String description;
    private String status;
    private Long facultyId;
    private Long adminId;
    private Long programId; // This is the most important field for your scan
}