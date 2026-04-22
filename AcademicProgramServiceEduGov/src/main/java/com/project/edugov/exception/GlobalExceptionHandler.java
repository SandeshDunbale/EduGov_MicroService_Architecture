package com.project.edugov.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// Extracts clean URI path from request
	private String getPath(WebRequest request) {
		return request.getDescription(false).replace("uri=", "");
	}

	// Handles 404 - Resource missing
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorDetails> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 404, "Not Found", ex.getMessage(), getPath(request));
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
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

	// Handles connectivity issues between microservices
	@ExceptionHandler(feign.FeignException.class)
	public ResponseEntity<ErrorDetails> handleFeignException(feign.FeignException ex, WebRequest request) {
		HttpStatus status = HttpStatus.resolve(ex.status()) != null ? HttpStatus.valueOf(ex.status())
				: HttpStatus.SERVICE_UNAVAILABLE;
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), status.value(), "External Service Error",
				"Remote service is currently unreachable.", getPath(request));
		return new ResponseEntity<>(error, status);
	}

	// Catch-all for unexpected server errors
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
		ErrorDetails error = new ErrorDetails(LocalDateTime.now(), 500, "Internal Server Error", ex.getMessage(),
				getPath(request));
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}