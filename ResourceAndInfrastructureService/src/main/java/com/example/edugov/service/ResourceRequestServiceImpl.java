package com.example.edugov.service;

import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.edugov.feign.UserClient;
import com.example.edugov.dto.UserDTO;
import com.example.edugov.model.*;
import com.example.edugov.repository.*;

@Slf4j
@Service
@Transactional
public class ResourceRequestServiceImpl implements ResourceRequestService {

    private final ResourceRequestRepository requestRepo;
    private final ResourceRepository resourceRepo;
    private final InfrastructureRepository infraRepo;
    private final ResourceService resourceService;
    private final InfrastructureService infrastructureService;
    private final UserClient userClient;

    public ResourceRequestServiceImpl(
            ResourceRequestRepository requestRepo,
            ResourceRepository resourceRepo,
            InfrastructureRepository infraRepo,
            ResourceService resourceService,
            InfrastructureService infrastructureService,
            UserClient userClient
    ) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.infraRepo = infraRepo;
        this.resourceService = resourceService;
        this.infrastructureService = infrastructureService;
        this.userClient = userClient;
        log.info("✅ ResourceRequestServiceImpl initialized");
    }

    // =====================================================
    // ROLE VALIDATION (via User Service)
    // =====================================================
    private void validateRole(Long userId, RequestItemType type) {

        UserDTO user = userClient.getUserById(userId);

        if (type == RequestItemType.RESOURCE && !"STUDENT".equals(user.role())) {
            throw new IllegalStateException("Only STUDENT can submit RESOURCE requests");
        }

        if (type == RequestItemType.INFRASTRUCTURE && !"FACULTY".equals(user.role())) {
            throw new IllegalStateException("Only FACULTY can submit INFRASTRUCTURE requests");
        }
    }

    // =====================================================
    // SUBMIT RESOURCE REQUEST
    // =====================================================
    @Override
    public ResourceRequest submitResourceRequest(
            Long requesterUserId,
            Long resourceId,
            int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        validateRole(requesterUserId, RequestItemType.RESOURCE);

        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Resource not found: " + resourceId));

        ResourceRequest rr = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .resource(resource)
                .itemType(RequestItemType.RESOURCE)
                .quantity(quantity)
                .status(RequestStatus.SUBMITTED)
                .build();

        return requestRepo.save(rr);
    }

    // =====================================================
    // SUBMIT INFRASTRUCTURE REQUEST
    // =====================================================
    @Override
    public ResourceRequest submitInfrastructureRequest(
            Long requesterUserId,
            Long infraId) {

        validateRole(requesterUserId, RequestItemType.INFRASTRUCTURE);

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Infrastructure not found: " + infraId));

        ResourceRequest rr = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .infrastructure(infra)
                .itemType(RequestItemType.INFRASTRUCTURE)
                .status(RequestStatus.SUBMITTED)
                .build();

        return requestRepo.save(rr);
    }

    // =====================================================
    // MARK IN REVIEW
    // =====================================================
    @Override
    public ResourceRequest markInReview(
            Long requestId,
            Long reviewerUserId) {

        userClient.getUserById(reviewerUserId); // validate exists

        ResourceRequest rr = getById(requestId);

        if (rr.getStatus() != RequestStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED requests can be reviewed");
        }

        rr.setStatus(RequestStatus.IN_REVIEW);
        rr.setApprovedByUserId(reviewerUserId);
        rr.setDecisionAt(Instant.now());

        return requestRepo.save(rr);
    }

    // =====================================================
    // APPROVE
    // =====================================================
    @Override
    public ResourceRequest approve(Long requestId, Long approverUserId) {

        userClient.getUserById(approverUserId);

        ResourceRequest rr = getById(requestId);

        if (rr.getStatus() != RequestStatus.IN_REVIEW &&
            rr.getStatus() != RequestStatus.SUBMITTED) {
            return rr;
        }

        if (rr.getItemType() == RequestItemType.RESOURCE) {
            resourceService.allocate(
                    rr.getResource().getResourceId(),
                    rr.getQuantity()
            );
        } else {
            infrastructureService.markInUse(
                    rr.getInfrastructure().getInfraId()
            );
        }

        rr.setStatus(RequestStatus.APPROVED);
        rr.setApprovedByUserId(approverUserId);
        rr.setDecisionAt(Instant.now());

        return requestRepo.save(rr);
    }

    // =====================================================
    // DECLINE
    // =====================================================
    @Override
    public ResourceRequest decline(
            Long requestId,
            Long approverUserId,
            String reasonOptional) {

        userClient.getUserById(approverUserId);

        ResourceRequest rr = getById(requestId);

        rr.setStatus(RequestStatus.DECLINED);
        rr.setApprovedByUserId(approverUserId);
        rr.setDecisionAt(Instant.now());

        return requestRepo.save(rr);
    }

    // =====================================================
    // QUERY METHODS
    // =====================================================
    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequest> listByStatus(RequestStatus status) {
        return requestRepo.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequest> listByRequester(Long requesterUserId) {
        return requestRepo.findByRequesterUserId(requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceRequest getById(Long requestId) {
        return requestRepo.findById(requestId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Request not found: " + requestId));
    }
}