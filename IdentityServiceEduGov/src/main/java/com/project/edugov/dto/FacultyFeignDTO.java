package com.project.edugov.dto;

import com.project.edugov.model.Status;

import lombok.Data;

@Data
public class FacultyFeignDTO {
	private Long facultyId;
	private String name;
	private String email;
	private String department;
	private Status status;
}