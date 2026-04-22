package com.project.edugov.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDetails {
	private LocalDateTime timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
}