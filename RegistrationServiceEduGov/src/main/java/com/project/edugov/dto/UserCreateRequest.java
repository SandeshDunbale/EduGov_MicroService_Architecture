package com.project.edugov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    private String email;
    private String password;
    private String name;
    private String role;
    private String phone ;// You will pass "STUDENT" or "FACULTY" here
}
