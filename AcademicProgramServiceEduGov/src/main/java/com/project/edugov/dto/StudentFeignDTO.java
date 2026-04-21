package com.project.edugov.dto;

import com.project.edugov.model.Status;

import lombok.Data;

@Data
public class StudentFeignDTO {
	private Long studentId;
	private String name;
	private String email;
	private Status status; // Uses your Status Enum
}