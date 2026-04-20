package com.project.edugov.service;

import com.project.edugov.dto.DocumentResponse;
import com.project.edugov.model.Document;
import com.project.edugov.model.Status;
import com.project.edugov.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository docRepo;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, Long userId, String userType, String docType, String docNum) throws IOException {
        
        Path targetLocation = Paths.get(uploadDir)
                .resolve(userType.toLowerCase())
                .resolve(userId.toString())
                .resolve(docType.toLowerCase());

        if (!Files.exists(targetLocation)) {
            Files.createDirectories(targetLocation);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = targetLocation.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Document doc = Document.builder()
                .userId(userId)
                .docType(docType.toUpperCase())
                .docNum(docNum)
                .fileUrl(filePath.toString())
                .verificationStatus(Status.PENDING)
                .uploadedDate(Instant.now())
                .build();

        Document savedDoc = docRepo.save(doc);

        // Convert Entity to DTO to fix Controller error
        return DocumentResponse.builder()
                .documentId(savedDoc.getUserId())
                .docType(savedDoc.getDocType())
                .docNum(savedDoc.getDocNum())
                .uploadStatus(savedDoc.getVerificationStatus().toString())
                .uploadedAt(savedDoc.getUploadedDate())
                .message("Document uploaded successfully to: " + userType)
                .build();
    }

    @Override
    @Transactional
    public DocumentResponse verifyDocument(Long docId, Status status, String notes, Long adminId) {
        Document doc = docRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + docId));

        doc.setVerificationStatus(status);
        doc.setAdminNotes(notes);
        doc.setVerifiedByUserId(adminId);
        doc.setVerifiedAt(Instant.now());

        Document updatedDoc = docRepo.save(doc);

        // Convert Entity to DTO to fix Controller error
        return DocumentResponse.builder()
                .documentId(updatedDoc.getUserId())
                .docType(updatedDoc.getDocType())
                .docNum(updatedDoc.getDocNum())
                .uploadStatus(updatedDoc.getVerificationStatus().toString())
                .uploadedAt(updatedDoc.getUploadedDate())
                .message("Document status updated to " + status)
                .build();
    }

    @Override
    public List<Document> getDocumentsByUserId(Long userId) {
        return docRepo.findByUserId(userId);
    }

    @Override
    public Document getDocumentById(Long docId) {
        return docRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + docId));
    }
}