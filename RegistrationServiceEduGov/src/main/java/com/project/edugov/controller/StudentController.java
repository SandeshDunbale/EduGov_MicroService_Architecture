package com.project.edugov.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> update(@PathVariable Long id, @Valid @RequestBody StudentDTO dto) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<StudentResponseDTO> approve(@PathVariable Long id) {
        log.info("REST: Approving student ID: {}", id);
        return ResponseEntity.ok(studentService.approveStudent(id));
    }

    @PatchMapping("/{id}/decline")
    public ResponseEntity<StudentResponseDTO> decline(@PathVariable Long id) {
        log.info("REST: Declining student ID: {}", id);
        return ResponseEntity.ok(studentService.declineStudent(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
