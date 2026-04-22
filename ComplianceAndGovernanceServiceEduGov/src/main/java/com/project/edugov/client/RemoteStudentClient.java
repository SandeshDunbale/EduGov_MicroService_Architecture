package com.project.edugov.client;

import com.project.edugov.dto.RemoteStudentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;


@FeignClient(
    name = "RegistrationServiceEduGov", 
    path = "/students"
)
public interface RemoteStudentClient {

    @GetMapping("/all")
    List<RemoteStudentDto> getAllStudents();
}

