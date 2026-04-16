package com.example.edugov.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================
    // NOT FOUND (404)
    // =========================================
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.error("404 - Not Found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // =========================================
    // BAD REQUEST (400)
    // =========================================
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        log.error("400 - Bad Request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // =========================================
    // VALIDATION ERROR (422)
    // =========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {

        Map<String, String> details = new HashMap<>();
        ex.getBindingResult()
          .getFieldErrors()
          .forEach(err -> details.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> body = base(HttpStatus.UNPROCESSABLE_ENTITY);
        body.put("message", "Validation failed");
        body.put("details", details);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {

        Map<String, String> details = new HashMap<>();
        ex.getConstraintViolations()
          .forEach(v -> details.put(v.getPropertyPath().toString(), v.getMessage()));

        Map<String, Object> body = base(HttpStatus.UNPROCESSABLE_ENTITY);
        body.put("message", "Constraint violation");
        body.put("details", details);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // =========================================
    // DATA CONFLICTS (409)
    // =========================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("409 - Data Integrity Violation", ex);
        return build(
                HttpStatus.CONFLICT,
                "Operation cannot be completed due to related data"
        );
    }

    // =========================================
    // FALLBACK (500)
    // =========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("500 - Internal Server Error", ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error"
        );
    }

    // =========================================
    // HELPERS
    // =========================================
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = base(status);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> base(HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return body;
    }
}