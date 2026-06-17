package com.project.edugov.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private String getPath(WebRequest request) {
		return request.getDescription(false).replace("uri=", "");
	}

	// FIX FOR ID > TABLE COUNT: Maps ResourceNotFoundException to 404 Not Found
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorDetails> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 404, "Not Found", ex.getMessage(), getPath(request));
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	// Maps AccountNotActive to 403 Forbidden
	@ExceptionHandler(AccountNotActiveException.class)
	public ResponseEntity<ErrorDetails> handleAccountNotActive(AccountNotActiveException ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 403, "Forbidden", ex.getMessage(), getPath(request));
		return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
	}

	// Maps InvalidCredentials to 401 Unauthorized
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorDetails> handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 401, "Unauthorized", ex.getMessage(),
				getPath(request));
		return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
	}

	// Catch-all for unexpected server crashes (returns 500)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 500, "Internal Server Error", ex.getMessage(),
				getPath(request));
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
