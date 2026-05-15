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
    
    // 1. INJECT THE LOGGER
    private final AsyncAuditLogger auditLogger; 

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, Long userId, String userType, String docType, String docNum) throws IOException {

        // ✅ Create folder path
        Path targetLocation = Paths.get(uploadDir)
                .resolve(userType.toLowerCase())
                .resolve(userId.toString())
                .resolve(docType.toLowerCase());

        if (!Files.exists(targetLocation)) {
            Files.createDirectories(targetLocation);
        }

        // ✅ Generate unique filename
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        Path filePath = targetLocation.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // ✅ Create URL (IMPORTANT FIX)
        String relativePath = userType.toLowerCase() + "/" + userId + "/" + docType.toLowerCase() + "/" + fileName;

        String fileUrl = "http://localhost:8002/api/documents/file/" + relativePath;

        // ✅ Save in DB
        Document doc = Document.builder()
                .userId(userId)
                .docType(docType.toUpperCase())
                .docNum(docNum)
                .fileUrl(fileUrl)   
                .verificationStatus(Status.PENDING)
                .uploadedDate(Instant.now())
                .build();

        Document savedDoc = docRepo.save(doc);

        // 2. FIRE THE AUDIT LOG (Background Thread)
        // We use the userId because the Student/Faculty performed this action.
        auditLogger.fireAndForgetLog(userId, "UPLOAD_DOCUMENT", "DocType: " + docType + ", DocNum: " + docNum);


        return DocumentResponse.builder()
                .documentId(savedDoc.getDocumentId())
                .docType(savedDoc.getDocType())
                .docNum(savedDoc.getDocNum())
                .file_url(savedDoc.getFileUrl())   // ✅ FIXED
                .uploadStatus(savedDoc.getVerificationStatus().toString())
                .uploadedAt(savedDoc.getUploadedDate())
                .message("Document uploaded successfully")
                .build();
    }

    @Override
    @Transactional
    public DocumentResponse verifyDocument(Long docId, Status status, String notes, Long adminId) {

        Document doc = docRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        doc.setVerificationStatus(status);
        doc.setAdminNotes(notes);
        doc.setVerifiedByUserId(adminId);
        doc.setVerifiedAt(Instant.now());

        Document updatedDoc = docRepo.save(doc);
        // 3. FIRE THE AUDIT LOG (Background Thread)
        // Notice we use adminId here! The Admin performed this action, not the document owner.
        auditLogger.fireAndForgetLog(adminId, "VERIFY_DOCUMENT", "Document ID: " + docId + " set to " + status);

        return DocumentResponse.builder()
                .documentId(updatedDoc.getDocumentId())
                .docType(updatedDoc.getDocType())
                .docNum(updatedDoc.getDocNum())
                .file_url(updatedDoc.getFileUrl())   // ✅ FIXED
                .uploadStatus(updatedDoc.getVerificationStatus().toString())
                .uploadedAt(updatedDoc.getUploadedDate())
                .message("Document status updated")
                .build();
    }

    @Override
    public List<Document> getDocumentsByUserId(Long userId) {
        return docRepo.findByUserId(userId);
    }

    @Override
    public Document getDocumentById(Long docId) {
        return docRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}
