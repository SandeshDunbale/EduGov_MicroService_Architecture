//package com.project.edugov.client;
//
//import com.project.edugov.dto.RemoteEnrollmentDto;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import java.util.List;
//
//@FeignClient(name = "ACADEMICPROGRAMSERVICEEDUGOV")
//public interface RemoteStudentClient {
//    // Add the context path here if you have one!
//    @GetMapping("/api/enrollments/all") 
//    List<RemoteEnrollmentDto> getAllEnrollments();
//}