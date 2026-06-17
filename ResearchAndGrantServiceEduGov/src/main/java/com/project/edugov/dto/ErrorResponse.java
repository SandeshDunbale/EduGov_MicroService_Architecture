package com.project.edugov.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String error,
    String message,
    LocalDateTime timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now());
    }

//	public ErrorResponse(String message2, int value, LocalDateTime now) {
//		// TODO Auto-generated constructor stub
//	}
}