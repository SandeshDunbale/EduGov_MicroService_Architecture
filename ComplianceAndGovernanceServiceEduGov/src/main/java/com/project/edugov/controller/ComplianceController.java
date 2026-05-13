package com.project.edugov.controller;

import com.project.edugov.dto.ComplianceRecordDTO;
import com.project.edugov.model.ComplianceRecord;
import com.project.edugov.service.ComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    @Autowired 
    private ComplianceService service;

    @PostMapping("/generate/{officerId}")
    public ResponseEntity<String> generate(@PathVariable Long officerId) {
        service.generateCompliance(officerId);
        return ResponseEntity.ok("Compliance scan initiated successfully. Check logs for violation details.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplianceRecordDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getComplianceById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<ComplianceRecordDTO> create(@RequestBody ComplianceRecord record) {
        if (record.getOfficerId() == null) {
            throw new IllegalArgumentException("Officer ID is missing from the request body");
        }
        return ResponseEntity.ok(service.createManual(record, record.getOfficerId()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ComplianceRecordDTO>> getAll() {
        return ResponseEntity.ok(service.getAllCompliance());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ComplianceRecordDTO> update(@PathVariable Long id, @RequestBody ComplianceRecord details) {
        return ResponseEntity.ok(service.updateCompliance(id, details));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteCompliance(id);
        return ResponseEntity.noContent().build();
    }
}