package com.project.edugov.service;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import com.project.edugov.dto.UserDTO;
import com.project.edugov.exception.DownstreamServiceUnavailableException;
import com.project.edugov.exception.RoleMismatchException;
import com.project.edugov.feign.NotificationClient;
import com.project.edugov.feign.UserClient;
import com.project.edugov.model.*;
import com.project.edugov.repository.InfrastructureRepository;
import com.project.edugov.repository.ResourceRepository;
import com.project.edugov.repository.ResourceRequestRepository;

@Slf4j
@Service
@Transactional
public class ResourceRequestServiceImpl implements ResourceRequestService {

    private final ResourceRequestRepository requestRepo;
    private final ResourceRepository resourceRepo;
    private final InfrastructureRepository infraRepo;

    private final ResourceService resourceService;
    private final InfrastructureService infrastructureService;

    // ✅ Microservice dependencies
    private final UserClient userClient;
    private final NotificationClient notificationClient;

    // ----------------------------------------------------
    // CONSTRUCTOR
    // ----------------------------------------------------
    public ResourceRequestServiceImpl(
            ResourceRequestRepository requestRepo,
            ResourceRepository resourceRepo,
            InfrastructureRepository infraRepo,
            ResourceService resourceService,
            InfrastructureService infrastructureService,
            UserClient userClient,
            NotificationClient notificationClient
    ) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.infraRepo = infraRepo;
        this.resourceService = resourceService;
        this.infrastructureService = infrastructureService;
        this.userClient = userClient;
        this.notificationClient = notificationClient;

        log.info("✅ ResourceRequestServiceImpl initialized");
    }

    // ====================================================
    // 🔐 USER SERVICE (HARD DEPENDENCY)
    // ====================================================
    @CircuitBreaker(name = "userService", fallbackMethod = "userFallback")
    private UserDTO fetchUser(Long userId) {
        return userClient.getUserById(userId);
    }

    private UserDTO userFallback(Long userId, Throwable ex) {
        log.error("Identity service DOWN. userId={}", userId, ex);
        throw new DownstreamServiceUnavailableException(
                "IDENTITYSERVICEEDUGOV",
                "Identity service is unavailable. Please try again later."
        );
    }

    // ====================================================
    // 🔔 NOTIFICATION SERVICE (SOFT DEPENDENCY)
    // ====================================================
    @CircuitBreaker(name = "notificationService", fallbackMethod = "notificationFallback")
    private void notifyUser(
            Long userId,
            Long entityId,
            String message,
            String category,
            String email
    ) {
        notificationClient.sendNotification(
                userId, entityId, message, category, email
        );
    }

    private void notificationFallback(
            Long userId,
            Long entityId,
            String message,
            String category,
            String email,
            Throwable ex
    ) {
        log.warn("Notification skipped. Service down. userId={}", userId);
        // ✅ DO NOT throw exception
    }

    // ----------------------------------------------------
    // ROLE VALIDATION (LOGIC UNCHANGED)
    // ----------------------------------------------------
    private void validateRole(Long userId, RequestItemType type) {

        UserDTO user = fetchUser(userId);

        if (!user.active()) {
            throw new IllegalStateException("User is inactive");
        }

        if (type == RequestItemType.RESOURCE &&
                !"STUDENT".equalsIgnoreCase(user.role())) {
            throw new RoleMismatchException(
                    "Only STUDENT can submit RESOURCE requests."
            );
        }

        if (type == RequestItemType.INFRASTRUCTURE &&
                !"FACULTY".equalsIgnoreCase(user.role())) {
            throw new RoleMismatchException(
                    "Only FACULTY can submit INFRASTRUCTURE requests."
            );
        }
    }

    // ----------------------------------------------------
    // SUBMIT RESOURCE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest submitResourceRequest(
            Long requesterUserId,
            Long resourceId,
            int quantity
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        validateRole(requesterUserId, RequestItemType.RESOURCE);

        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Resource not found"));

        ResourceRequest request = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .resource(resource)
                .itemType(RequestItemType.RESOURCE)
                .quantity(quantity)
                .status(RequestStatus.SUBMITTED)
                .build();

        ResourceRequest saved = requestRepo.save(request);

        UserDTO requester = fetchUser(requesterUserId);
        notifyUser(
                requesterUserId,
                saved.getRequestId(),
                "Resource request submitted successfully",
                "RESOURCE_REQUEST",
                requester.email()
        );

        return saved;
    }

    // ----------------------------------------------------
    // SUBMIT INFRASTRUCTURE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest submitInfrastructureRequest(
            Long requesterUserId,
            Long infraId
    ) {

        validateRole(requesterUserId, RequestItemType.INFRASTRUCTURE);

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Infrastructure not found"));

        ResourceRequest request = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .infrastructure(infra)
                .itemType(RequestItemType.INFRASTRUCTURE)
                .status(RequestStatus.SUBMITTED)
                .build();

        ResourceRequest saved = requestRepo.save(request);

        UserDTO requester = fetchUser(requesterUserId);
        notifyUser(
                requesterUserId,
                saved.getRequestId(),
                "Infrastructure request submitted successfully",
                "INFRA_REQUEST",
                requester.email()
        );

        return saved;
    }

    // ----------------------------------------------------
    // APPROVE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest approve(Long requestId, Long approverUserId) {

        fetchUser(approverUserId); // validate approver exists

        ResourceRequest request = getById(requestId);

        if (request.getItemType() == RequestItemType.RESOURCE) {
            resourceService.allocate(
                    request.getResource().getResourceId(),
                    request.getQuantity()
            );
        } else {
            infrastructureService.markInUse(
                    request.getInfrastructure().getInfraId()
            );
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedByUserId(approverUserId);
        request.setDecisionAt(Instant.now());

        ResourceRequest saved = requestRepo.save(request);

        UserDTO requester = fetchUser(request.getRequesterUserId());
        notifyUser(
                requester.userId(),
                saved.getRequestId(),
                "Your request has been APPROVED",
                "REQUEST_DECISION",
                requester.email()
        );

        return saved;
    }

    // ----------------------------------------------------
    // DECLINE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest decline(
            Long requestId,
            Long approverUserId,
            String reason
    ) {

        fetchUser(approverUserId);

        ResourceRequest request = getById(requestId);

        request.setStatus(RequestStatus.DECLINED);
        request.setApprovedByUserId(approverUserId);
        request.setDecisionAt(Instant.now());

        // ✅ SET BEFORE SAVE (IMPORTANT)
        request.setReason(reason);

        ResourceRequest saved = requestRepo.save(request);

        UserDTO requester = fetchUser(request.getRequesterUserId());

        notifyUser(
                requester.userId(),
                saved.getRequestId(),
                "Your request was DECLINED: " + reason,
                "REQUEST_DECISION",
                requester.email()
        );

        return saved;
    }

    // ----------------------------------------------------
    // READ OPERATIONS
    // ----------------------------------------------------
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
                        new EntityNotFoundException("Request not found"));
    }

    // ----------------------------------------------------
    // MARK IN REVIEW
    // ----------------------------------------------------
    @Override
    public ResourceRequest markInReview(
            Long requestId,
            Long reviewerUserId
    ) {

        fetchUser(reviewerUserId);

        ResourceRequest request = getById(requestId);

        request.setStatus(RequestStatus.IN_REVIEW);
        request.setApprovedByUserId(reviewerUserId);
        request.setDecisionAt(Instant.now());

        return requestRepo.save(request);
    }
}