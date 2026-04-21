package com.project.edugov.exception;
public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(String message) { super(message); }
}