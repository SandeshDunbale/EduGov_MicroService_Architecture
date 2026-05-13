package com.project.edugov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Resource;
import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;

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
    List<Resource> findByProgramIdAndType(Long programId, ResourceType type);


    long countByProgramIdAndStatus(Long programId, ResourceStatus status);
}