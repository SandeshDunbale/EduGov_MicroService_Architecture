package com.project.edugov.dto;

import lombok.Data;

@Data
public class AuditLogRequest {
    private Long userId;
    private String action;
    private String resource;
}