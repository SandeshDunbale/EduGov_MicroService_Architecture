package com.project.edugov.repository;

import com.project.edugov.model.Audit;
// Removed direct User reference to support remote user lookups via Feign
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findByStatus(String status);

    List<Audit> findByOfficerId(Long officerId);
    
    List<Audit> findByScopeContainingIgnoreCase(String keyword);
}
