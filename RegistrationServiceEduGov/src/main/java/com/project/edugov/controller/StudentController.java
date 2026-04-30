package com.project.edugov.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.edugov.dto.StudentDTO;
import com.project.edugov.dto.StudentResponseDTO;
import com.project.edugov.model.Status;
import com.project.edugov.service.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<StudentResponseDTO> register(@Valid @RequestBody StudentDTO studentDTO) {
        log.info("REST: Registering new student: {}", studentDTO.getEmail());
        StudentResponseDTO response = studentService.registerStudent(studentDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getById(@PathVariable Long id) {
        return studentService.getStudentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<StudentResponseDTO>> getByStatus(@PathVariable Status status) {
        return ResponseEntity.ok(studentService.getStudentsByStatus(status));
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<StudentResponseDTO> update(@PathVariable Long id, @Valid @RequestBody StudentDTO dto) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    @PatchMapping("/{id}/approve") // Ensure this is lowercase
    public ResponseEntity<StudentResponseDTO> approveStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.approveStudent(id));
    }
    
    @PatchMapping("/{id}/decline")
    public ResponseEntity<StudentResponseDTO> decline(@PathVariable Long id) {
        log.info("REST: Declining student ID: {}", id);
        return ResponseEntity.ok(studentService.declineStudent(id));
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        String message = studentService.deleteStudent(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Faculty with ID " + id + " has been successfully deleted.");
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }
    
    //Module 6 requirements
 // Add this to StudentController.java
    @GetMapping("/all")
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        log.info("API Hit: GET /students/all | Fetching all students for compliance scan");
        return ResponseEntity.ok(studentService.getAllStudents());
    }
}
