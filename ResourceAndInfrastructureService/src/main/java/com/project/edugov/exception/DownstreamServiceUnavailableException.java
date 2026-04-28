package com.project.edugov.exception;

public class DownstreamServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public DownstreamServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}