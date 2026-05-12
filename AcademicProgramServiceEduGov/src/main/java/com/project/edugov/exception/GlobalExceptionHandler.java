package com.project.edugov.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// Extracts clean URI path from request
	private String getPath(WebRequest request) {
		return request.getDescription(false).replace("uri=", "");
	}

	// 1. RESILIENCE4J: Handle Open Circuit (Circuit is Tripped)
	@ExceptionHandler(CallNotPermittedException.class)
	public ResponseEntity<ErrorDetails> handleCallNotPermittedException(CallNotPermittedException ex,
			WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 503, "Service Unavailable",
				"The dependent service is currently unavailable due to high failure rates (Circuit Open).",
				getPath(request));
		return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
	}

	// 2. RESILIENCE4J: Handle Rate Limiting
	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<ErrorDetails> handleRequestNotPermitted(RequestNotPermitted ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 429, "Too Many Requests",
				"Rate limit exceeded. Please wait before trying again.", getPath(request));
		return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
	}

	// 3. VALIDATION: Handle @Valid annotation failures
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorDetails> handleValidationException(MethodArgumentNotValidException ex,
			WebRequest request) {
		String details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining(", "));

		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 400, "Validation Failed", details, getPath(request));
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	// Handles 404 - Resource missing
	// In IDENTITY SERVICE GlobalExceptionHandler.java
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorDetails> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
	    ErrorDetails error = new ErrorDetails(
	        LocalDateTime.now(), 
	        HttpStatus.NOT_FOUND.value(), 
	        "Not Found", 
	        ex.getMessage(), 
	        getPath(request)
	    );
	    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND); // CRITICAL: This must be 404
	}
	// Handles custom business logic errors
	@ExceptionHandler(APIException.class)
	public ResponseEntity<ErrorDetails> handleAPIException(APIException ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), ex.getStatus().value(),
				ex.getStatus().getReasonPhrase(), ex.getMessage(), getPath(request));
		return new ResponseEntity<>(error, ex.getStatus());
	}

	// Handles specific enrollment processing errors
	@ExceptionHandler(EnrollmentException.class)
	public ResponseEntity<ErrorDetails> handleEnrollmentException(EnrollmentException ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 400, "Bad Request", ex.getMessage(),
				getPath(request));
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	// Catch-all for unexpected server errors
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 500, "Internal Server Error", ex.getMessage(),
				getPath(request));
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}