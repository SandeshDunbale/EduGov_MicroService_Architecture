package com.project.edugov.repository;

import com.project.edugov.model.ComplianceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplianceRepository extends JpaRepository<ComplianceRecord, Long> {
    
    boolean existsByEntityIdAndEntityType(Long entityId, String entityType);
}
