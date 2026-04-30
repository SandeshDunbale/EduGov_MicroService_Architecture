package com.project.edugov.service;

import com.project.edugov.dto.DocumentResponse; // Use the DTO
import com.project.edugov.model.Document;
import com.project.edugov.model.Status;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file, 
                                    Long userId, 
                                    String userType, 
                                    String docType, 
                                    String docNum) throws IOException;

    DocumentResponse verifyDocument(Long docId, 
                                    Status status, 
                                    String notes, 
                                    Long adminId);

    List<Document> getDocumentsByUserId(Long userId);

    Document getDocumentById(Long docId);
}