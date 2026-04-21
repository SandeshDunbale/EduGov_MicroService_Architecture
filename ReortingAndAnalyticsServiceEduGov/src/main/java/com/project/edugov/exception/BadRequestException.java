package com.project.edugov.exception;
 
public class BadRequestException extends RuntimeException {
 
    public BadRequestException(String message) {
        super(message);
    }
}