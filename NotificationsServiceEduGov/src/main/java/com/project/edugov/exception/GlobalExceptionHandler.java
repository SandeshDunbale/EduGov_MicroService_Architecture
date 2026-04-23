package com.project.edugov.exception;
 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
 
@RestControllerAdvice
public class GlobalExceptionHandler {
 
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
 
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", 404);
        error.put("message", ex.getMessage());
 
        return ResponseEntity.status(404).body(error);
    }
}