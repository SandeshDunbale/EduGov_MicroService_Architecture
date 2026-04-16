package com.example.edugov.repository;

import com.example.edugov.model.Resource;
import com.example.edugov.model.ResourceStatus;
import com.example.edugov.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository
        extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    // =========================
    // BASIC FILTERS
    // =========================
    List<Resource> findByProgramId(Long programId);

    List<Resource> findByStatus(ResourceStatus status);

    List<Resource> findByType(ResourceType type);

    // =========================
    // COMBINED FILTERS
    // =========================
    List<Resource> findByProgramIdAndStatus(Long programId, ResourceStatus status);

    List<Resource> findByTypeAndStatus(ResourceType type, ResourceStatus status);

    Optional<Resource> findFirstByProgramIdAndTypeAndStatus(
            Long programId, ResourceType type, ResourceStatus status
    );

    long countByProgramIdAndStatus(Long programId, ResourceStatus status);
}