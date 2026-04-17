package com.project.edugov.dto;

import com.project.edugov.model.Role;

import lombok.Data;

@Data
public class UserFeignDTO {
	private Long userId;
	private String name;
	private String email;
	private Role role; // Uses your Role Enum (UNIV_ADMIN, etc.)
}