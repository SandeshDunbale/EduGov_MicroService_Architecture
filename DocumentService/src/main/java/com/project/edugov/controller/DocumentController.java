package com.project.edugov.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.edugov.dto.DocumentResponse;
import com.project.edugov.model.Document;
import com.project.edugov.model.Status;
import com.project.edugov.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService docService;

	// Change the mapping to include {userType}
	@PostMapping("/{userType}/upload")
	public ResponseEntity<DocumentResponse> upload(
	        @RequestParam("file") MultipartFile file,
	        @RequestParam Long userId,
	        @PathVariable String userType, // Changed from @RequestParam to @PathVariable
	        @RequestParam String docType,
	        @RequestParam String docNum) throws IOException {
	    
	    DocumentResponse response = docService.uploadDocument(file, userId, userType, docType, docNum);
	    return ResponseEntity.ok(response);
	}

    @PatchMapping("/verify/{docId}")
    public ResponseEntity<DocumentResponse> verify(@PathVariable Long docId,
                                                   @RequestParam Status status,
                                                   @RequestParam String notes,
                                                   @RequestParam Long adminId) {
        // We call the service method here
        DocumentResponse response = docService.verifyDocument(docId, status, notes, adminId);
        return ResponseEntity.ok(response);
    }
    
    
 // 1. Get all documents for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(@PathVariable Long userId) {
        List<Document> documents = docService.getDocumentsByUserId(userId);
        
        // Map the list of entities to a list of DTOs
        List<DocumentResponse> response = documents.stream()
            .map(doc -> DocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .docType(doc.getDocType())
                .docNum(doc.getDocNum())
                .uploadStatus(doc.getVerificationStatus().toString())
                .uploadedAt(doc.getUploadedDate())
                .message("Fetched from database")
                .build())
            .toList();

        return ResponseEntity.ok(response);
    }

    // 2. Get a single document by its ID
    @GetMapping("/{docId}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long docId) {
        Document doc = docService.getDocumentById(docId);
        
        DocumentResponse response = DocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .docType(doc.getDocType())
                .docNum(doc.getDocNum())
                .uploadStatus(doc.getVerificationStatus().toString())
                .uploadedAt(doc.getUploadedDate())
                .message("Document found")
                .build();

        return ResponseEntity.ok(response);
    }
}