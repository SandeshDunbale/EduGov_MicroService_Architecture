package com.example.edugov.repository;

import com.example.edugov.model.Infrastructure;
import com.example.edugov.model.InfrastructureStatus;
import com.example.edugov.model.InfrastructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfrastructureRepository
        extends JpaRepository<Infrastructure, Long>,
                JpaSpecificationExecutor<Infrastructure> {

    // =========================
    // BASIC FILTERS
    // =========================
    List<Infrastructure> findByProgramId(Long programId);

    List<Infrastructure> findByStatus(InfrastructureStatus status);

    List<Infrastructure> findByType(InfrastructureType type);

    // =========================
    // COMBINED FILTERS
    // =========================
    List<Infrastructure> findByProgramIdAndType(
            Long programId,
            InfrastructureType type
    );

    List<Infrastructure> findByProgramIdAndStatus(
            Long programId,
            InfrastructureStatus status
    );

    List<Infrastructure> findByTypeAndCapacityGreaterThanEqual(
            InfrastructureType type,
            Integer minCapacity
    );
}
