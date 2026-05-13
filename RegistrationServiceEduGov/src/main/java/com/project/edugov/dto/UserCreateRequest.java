package com.project.edugov.dto;

import java.time.LocalDate;

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
    private String phone ;
    private LocalDate dob;
   // You will pass "STUDENT" or "FACULTY" here
}
