package com.example.edugov.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.edugov.model.Infrastructure;
import com.example.edugov.model.RequestItemType;
import com.example.edugov.model.RequestStatus;
import com.example.edugov.model.Resource;
import com.example.edugov.model.ResourceRequest;

@Repository
public interface ResourceRequestRepository
        extends JpaRepository<ResourceRequest, Long>,
                JpaSpecificationExecutor<ResourceRequest> {

    // =========================
    // STATUS BASED
    // =========================
    List<ResourceRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    List<ResourceRequest> findByStatus(RequestStatus status);

    long countByStatus(RequestStatus status);

    // =========================
    // USER BASED (ID ONLY ✅)
    // =========================
    List<ResourceRequest> findByRequesterUserId(Long requesterUserId);

    List<ResourceRequest> findByRequesterUserIdAndStatus(
            Long requesterUserId,
            RequestStatus status
    );

    long countByRequesterUserIdAndStatus(
            Long requesterUserId,
            RequestStatus status
    );

    // =========================
    // ITEM TYPE BASED
    // =========================
    List<ResourceRequest> findByItemType(RequestItemType itemType);

    List<ResourceRequest> findByItemTypeAndStatus(
            RequestItemType itemType,
            RequestStatus status
    );

    // =========================
    // RESOURCE BASED (LOCAL JPA ✅)
    // =========================
    List<ResourceRequest> findByResource(Resource resource);

    List<ResourceRequest> findByResourceAndStatus(
            Resource resource,
            RequestStatus status
    );

    long countByResourceAndStatusIn(
            Resource resource,
            Collection<RequestStatus> statuses
    );

    // =========================
    // INFRASTRUCTURE BASED (LOCAL JPA ✅)
    // =========================
    List<ResourceRequest> findByInfrastructure(Infrastructure infrastructure);

    List<ResourceRequest> findByInfrastructureAndStatus(
            Infrastructure infrastructure,
            RequestStatus status
    );

    long countByInfrastructureAndStatusIn(
            Infrastructure infrastructure,
            Collection<RequestStatus> statuses
    );

    // =========================
    // DATE BASED
    // =========================
    List<ResourceRequest> findByCreatedAtBetween(Instant from, Instant to);

    List<ResourceRequest> findByDecisionAtBetween(Instant from, Instant to);
}