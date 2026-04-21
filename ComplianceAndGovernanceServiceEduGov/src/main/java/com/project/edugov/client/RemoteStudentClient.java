package com.project.edugov.client;

import com.project.edugov.dto.RemoteStudentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "AcademicProgramServiceEduGov", path = "/api/students")
public interface RemoteStudentClient {

    @GetMapping
    List<RemoteStudentDto> getAllStudents();
}
