package com.project.edugov.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.EnrollmentResponseDTO;
import com.project.edugov.model.Status;
import com.project.edugov.service.EnrollmentService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/enrollments")
@Slf4j
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @PostMapping("/apply")
    public ResponseEntity<EnrollmentResponseDTO> apply(@RequestBody Map<String, Long> request) {
        return new ResponseEntity<>(enrollmentService.applyForCourse(
                request.get("studentId"), request.get("courseId")), HttpStatus.CREATED);
    }

    @PutMapping("/update/{eId}/admin/{aId}/status/{status}")
    public ResponseEntity<EnrollmentResponseDTO> updateEnrollment(
            @PathVariable Long eId, @PathVariable Long aId, @PathVariable String status) {
        
        Status enrollmentStatus = Status.valueOf(status.toUpperCase());
        return ResponseEntity.ok(enrollmentService.updateEnrollmentStatus(eId, aId, enrollmentStatus));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStatus(Status.valueOf(status.toUpperCase())));
    }

    @GetMapping("/all")
    public ResponseEntity<List<EnrollmentResponseDTO>> getAll() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
}