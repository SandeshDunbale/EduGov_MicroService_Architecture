package com.project.edugov.exception;
 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDateTime;
 
@RestControllerAdvice
public class GlobalExceptionHandler {
 
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
 
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 404, ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }
 
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
 
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 400, ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }
 
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
 
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 500, ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
 