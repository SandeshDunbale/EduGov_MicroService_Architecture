package com.project.edugov.exception;

public class RoleMismatchException extends RuntimeException {
    public RoleMismatchException(String message) {
        super(message);
    }
}