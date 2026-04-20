package com.project.edugov.repository;

import com.project.edugov.model.Document;
import com.project.edugov.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

   
    List<Document> findByUserId(Long userId);

    
    List<Document> findByVerificationStatus(Status status);

   
    List<Document> findByUserIdAndDocType(Long userId, String docType);
}