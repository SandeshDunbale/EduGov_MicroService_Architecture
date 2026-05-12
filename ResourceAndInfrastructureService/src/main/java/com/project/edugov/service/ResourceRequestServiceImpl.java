package com.project.edugov.service;

import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.dto.UserDTO;
import com.project.edugov.feign.UserClient;
import com.project.edugov.exception.RoleMismatchException;
import com.project.edugov.model.*;
import com.project.edugov.repository.*;

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
    
    // 1. INJECT THE ASYNC AUDIT LOGGER
    private final AsyncAuditLogger auditLogger;

    public ResourceRequestServiceImpl(
            ResourceRequestRepository requestRepo,
            ResourceRepository resourceRepo,
            InfrastructureRepository infraRepo,
            ResourceService resourceService,
            InfrastructureService infrastructureService,
            UserClient userClient,
            AsyncAuditLogger auditLogger
    ) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.infraRepo = infraRepo;
        this.resourceService = resourceService;
        this.infrastructureService = infrastructureService;
        this.userClient = userClient;
        this.auditLogger = auditLogger;

        log.info("ResourceRequestServiceImpl initialized");
    }

    private void validateRole(Long userId, RequestItemType type) {
        UserDTO user = userClient.getUserById(userId);

        if (!user.active()) {
            throw new IllegalStateException("User is inactive");
        }

        if (type == RequestItemType.RESOURCE && !"STUDENT".equalsIgnoreCase(user.role())) {
            throw new RoleMismatchException("Only STUDENT can submit RESOURCE requests.");
        }

        if (type == RequestItemType.INFRASTRUCTURE && !"FACULTY".equalsIgnoreCase(user.role())) {
            throw new RoleMismatchException("Only FACULTY can submit INFRASTRUCTURE requests.");
        }
    }

    @Override
    public ResourceRequest submitResourceRequest(Long requesterUserId, Long resourceId, int quantity) {
        log.info("Submitting Resource Request → requesterId={}, resourceId={}, qty={}", requesterUserId, resourceId, quantity);

        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0.");

        validateRole(requesterUserId, RequestItemType.RESOURCE);

        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found: " + resourceId));

        ResourceRequest rr = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .resource(resource)
                .itemType(RequestItemType.RESOURCE)
                .quantity(quantity)
                .status(RequestStatus.SUBMITTED)
                .build();

        ResourceRequest saved = requestRepo.save(rr);

        // 2. FIRE AUDIT LOG (Student Action)
        auditLogger.fireAndForgetLog(requesterUserId, "SUBMIT_RESOURCE_REQUEST", "Resource ID: " + resourceId + ", Qty: " + quantity);

        return saved;
    }

    @Override
    public ResourceRequest submitInfrastructureRequest(Long requesterUserId, Long infraId) {
        validateRole(requesterUserId, RequestItemType.INFRASTRUCTURE);

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() -> new EntityNotFoundException("Infrastructure not found: " + infraId));

        ResourceRequest rr = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .infrastructure(infra)
                .itemType(RequestItemType.INFRASTRUCTURE)
                .status(RequestStatus.SUBMITTED)
                .build();

        ResourceRequest saved = requestRepo.save(rr);

        // 3. FIRE AUDIT LOG (Faculty Action)
        auditLogger.fireAndForgetLog(requesterUserId, "SUBMIT_INFRASTRUCTURE_REQUEST", "Infra ID: " + infraId);

        return saved;
    }

    @Override
    public ResourceRequest approve(Long requestId, Long approverUserId) {
        userClient.getUserById(approverUserId);
        ResourceRequest rr = getById(requestId);

        if (rr.getItemType() == RequestItemType.RESOURCE) {
            resourceService.allocate(rr.getResource().getResourceId(), rr.getQuantity());
        } else {
            infrastructureService.markInUse(rr.getInfrastructure().getInfraId());
        }

        rr.setStatus(RequestStatus.APPROVED);
        rr.setApprovedByUserId(approverUserId);
        rr.setDecisionAt(Instant.now());

        ResourceRequest saved = requestRepo.save(rr);

        // 4. FIRE AUDIT LOG (Manager Action)
        auditLogger.fireAndForgetLog(approverUserId, "APPROVE_REQUEST", "Request ID: " + requestId);

        return saved;
    }

    @Override
    public ResourceRequest decline(Long requestId, Long approverUserId, String reason) {
        userClient.getUserById(approverUserId);
        ResourceRequest rr = getById(requestId);

        rr.setStatus(RequestStatus.DECLINED);
        rr.setApprovedByUserId(approverUserId);
        rr.setDecisionAt(Instant.now());

        ResourceRequest saved = requestRepo.save(rr);

        // 5. FIRE AUDIT LOG (Manager Action)
        auditLogger.fireAndForgetLog(approverUserId, "DECLINE_REQUEST", "Request ID: " + requestId + ". Reason: " + reason);

        return saved;
    }

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
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));
    }

    @Override
    public ResourceRequest markInReview(Long requestId, Long reviewerUserId) {
        userClient.getUserById(reviewerUserId);
        ResourceRequest rr = getById(requestId);

        rr.setStatus(RequestStatus.IN_REVIEW);
        rr.setApprovedByUserId(reviewerUserId);
        rr.setDecisionAt(Instant.now());

        ResourceRequest saved = requestRepo.save(rr);

        // 6. FIRE AUDIT LOG (Reviewer Action)
        auditLogger.fireAndForgetLog(reviewerUserId, "REQUEST_IN_REVIEW", "Request ID: " + requestId);

        return saved;
    }
}