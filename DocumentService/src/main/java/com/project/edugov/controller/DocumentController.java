package com.project.edugov.controller;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.edugov.dto.DocumentResponse;
import com.project.edugov.model.Document;
import com.project.edugov.model.Status;
import com.project.edugov.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService docService;

    // ✅ Upload
    @PostMapping("/{userType}/upload")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long userId,
            @PathVariable String userType,
            @RequestParam String docType,
            @RequestParam String docNum) throws IOException {

        DocumentResponse response = docService.uploadDocument(file, userId, userType, docType, docNum);
        return ResponseEntity.ok(response);
    }

    // ✅ Verify
    @PatchMapping("/verify/{docId}")
    public ResponseEntity<DocumentResponse> verify(@PathVariable Long docId,
                                                   @RequestParam Status status,
                                                   @RequestParam String notes,
                                                   @RequestParam Long adminId) {

        DocumentResponse response = docService.verifyDocument(docId, status, notes, adminId);
        return ResponseEntity.ok(response);
    }

    // ✅ Get all docs for user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(@PathVariable Long userId) {
        // 1. Fetch from service
        List<Document> documents = docService.getDocumentsByUserId(userId);

        // 2. Safety Check: If list is null, return empty list instead of crashing
        if (documents == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        List<DocumentResponse> response = documents.stream()
            .map(doc -> DocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .docType(doc.getDocType())
                .docNum(doc.getDocNum())
                .file_url(doc.getFileUrl())
                // 3. Safety Check: Handle potential null status
                .uploadStatus(doc.getVerificationStatus() != null ? doc.getVerificationStatus().toString() : "PENDING")
                .uploadedAt(doc.getUploadedDate())
                .message("Fetched")
                .build())
            .toList();

        return ResponseEntity.ok(response);
    }

    // ✅ Get single doc
    @GetMapping("/{docId}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long docId) {

        Document doc = docService.getDocumentById(docId);

        DocumentResponse response = DocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .docType(doc.getDocType())
                .docNum(doc.getDocNum())
                .file_url(doc.getFileUrl())   // ✅ FIXED
                .uploadStatus(doc.getVerificationStatus().toString())
                .uploadedAt(doc.getUploadedDate())
                .message("Found")
                .build();

        return ResponseEntity.ok(response);
    }
    

    // ✅ ✅ NEW - VIEW FILE (MOST IMPORTANT)
 // Add this import
    @GetMapping("/file/**")
    public ResponseEntity<Resource> viewFile(HttpServletRequest request) throws IOException {
        // Extract everything after /file/
        String fullPath = request.getRequestURI(); 
        String searchTerm = "/api/documents/file/";
        String relativePath = fullPath.substring(fullPath.indexOf(searchTerm) + searchTerm.length());

        // Decode URL (in case there are spaces or special characters in filenames)
        relativePath = java.net.URLDecoder.decode(relativePath, java.nio.charset.StandardCharsets.UTF_8);

        Path filePath = Paths.get("C:/edugov-uploads").resolve(relativePath).normalize();

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }
    
    
    
    
    
 // NEW SEPARATE METHOD FOR STUDENTS/FACULTY
    @GetMapping("/view/**")
    public ResponseEntity<Resource> studentViewFile(HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI(); 
            // We use a different search term so Admin is never affected
            String searchTerm = "/api/documents/view/";
            String relativePath = fullPath.substring(fullPath.indexOf(searchTerm) + searchTerm.length());

            // Decode spaces and special characters for the Student's screenshots
            String decodedPath = java.net.URLDecoder.decode(relativePath, java.nio.charset.StandardCharsets.UTF_8);

            Path filePath = Paths.get("C:/edugov-uploads").resolve(decodedPath).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);

        } catch (Exception e) {
            // Only logs errors for the new student endpoint
            System.err.println("Student View Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}





