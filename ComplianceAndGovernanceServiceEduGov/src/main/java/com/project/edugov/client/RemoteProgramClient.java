package com.project.edugov.client;

import com.project.edugov.dto.RemoteCourseDto;
import com.project.edugov.dto.RemoteEnrollmentDto;
import com.project.edugov.dto.RemoteProgramDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "ACADEMICPROGRAMSERVICEEDUGOV") 
public interface RemoteProgramClient {

    @GetMapping("/programs/status/{status}")
    List<RemoteProgramDto> getByStatus(@PathVariable("status") String status);

    @GetMapping("/courses/all")
    List<RemoteCourseDto> getAllCourses();

    // MOVED HERE: Since Enrollment is in the Academic Module, it belongs to this client!
    @GetMapping("/enrollments/all")
    List<RemoteEnrollmentDto> getAllEnrollments();
}