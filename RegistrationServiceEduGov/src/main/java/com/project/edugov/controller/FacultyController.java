package com.project.edugov.controller;

import com.project.edugov.dto.FacultyDTO;
import com.project.edugov.dto.FacultyResponseDTO;
import com.project.edugov.model.Status;
import com.project.edugov.service.FacultyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService facultyService;

    // 1. Register a new Faculty (Triggers Identity Service internally)
    @PostMapping("/register")
    public ResponseEntity<FacultyResponseDTO> register(@RequestBody FacultyDTO dto) {
        return new ResponseEntity<>(facultyService.registerFaculty(dto), HttpStatus.CREATED);
    }

    // 2. Get Faculty by Local ID
    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponseDTO> getById(@PathVariable Long id) {
        return facultyService.getFacultyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Get all Faculties by Status (PENDING, APPROVED, REJECTED)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FacultyResponseDTO>> getByStatus(@PathVariable Status status) {
        return ResponseEntity.ok(facultyService.getFacultyByStatus(status));
    }

    // 4. Update Faculty Profile
    @PutMapping("/{id}/update")
    public ResponseEntity<FacultyResponseDTO> update(@PathVariable Long id, @RequestBody FacultyDTO dto) {
        return ResponseEntity.ok(facultyService.updateFaculty(id, dto));
    }

    // 5. Approve Faculty (Triggers Status Update in Identity Service)
    @PatchMapping("/{id}/approve")
    public ResponseEntity<FacultyResponseDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.approveFaculty(id));
    }

    // 6. Decline Faculty
    @PatchMapping("/{id}/decline")
    public ResponseEntity<FacultyResponseDTO> decline(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.declineFaculty(id));
    }


    
    
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        facultyService.deleteFaculty(id);
        
        // Create a response map
        Map<String, String> response = new HashMap<>();
        response.put("message", "Faculty with ID " + id + " has been successfully deleted.");
        response.put("status", "SUCCESS");
        
        return ResponseEntity.ok(response);
    }
}
