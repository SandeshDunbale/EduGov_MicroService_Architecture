package com.project.edugov.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FacultyMinimalDTO {
	private Long facultyId;
	
	// Tells Jackson to map the incoming JSON key "name" to this field
		@JsonProperty("name")	
	private String facultyName; // map from User
		@JsonProperty("email")
		private String email;
}