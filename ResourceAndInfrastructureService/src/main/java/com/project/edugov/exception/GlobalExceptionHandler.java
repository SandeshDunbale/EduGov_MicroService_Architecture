package com.project.edugov.exception;

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

import feign.FeignException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================
    // ✅ 404 - LOCAL ENTITY NOT FOUND
    // =========================================
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("404 - Not Found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // =========================================
    // ✅ NEW ✅ 404 - DOWNSTREAM NOT FOUND (Program NOT FOUND)
    // =========================================
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleFeignNotFound(FeignException.NotFound ex) {

        log.warn("404 - Downstream resource not found: {}", ex.getMessage());

        return build(
                HttpStatus.NOT_FOUND,
                "Requested resource not found in downstream service"
        );
    }

    // =========================================
    // ✅ 503 - DOWNSTREAM SERVICE DOWN
    // =========================================
    @ExceptionHandler(DownstreamServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleDownstreamServiceUnavailable(
            DownstreamServiceUnavailableException ex) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("service", ex.getServiceName());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    // =========================================
    // ✅ NEW ✅ 400 - VALIDATION ERRORS (JSON body)
    // =========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(error.getField(), error.getDefaultMessage())
                );

        Map<String, Object> body = base(HttpStatus.BAD_REQUEST);
        body.put("message", "Validation failed");
        body.put("details", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    // =========================================
    // ✅ NEW ✅ 400 - CONSTRAINT VIOLATION
    // =========================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {

        Map<String, Object> body = base(HttpStatus.BAD_REQUEST);
        body.put("message", ex.getMessage());

        return ResponseEntity.badRequest().body(body);
    }

    // =========================================
    // ✅ 400 - BUSINESS LOGIC ERRORS
    // =========================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("400 - Bad Request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("400 - Illegal State: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // =========================================
    // ✅ 403 - ROLE MISMATCH
    // =========================================
    @ExceptionHandler(RoleMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleRoleMismatch(RoleMismatchException ex) {
        log.warn("403 - Forbidden: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // =========================================
    // ✅ 409 - DATA INTEGRITY
    // =========================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("409 - Data Integrity Violation", ex);
        return build(HttpStatus.CONFLICT, "Operation cannot be completed due to related data");
    }

    // =========================================
    // ✅ 503 - FEIGN (GENERIC)
    // =========================================
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        log.error("503 - Feign error", ex);
        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Dependent service is currently unavailable. Please try again later."
        );
    }

    // =========================================
    // ✅ 500 - FALLBACK
    // =========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("500 - Internal Server Error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
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