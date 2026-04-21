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

    /*
     * ❌ MONOLITHIC SERVICES (COMMENTED FOR MICROSERVICE)
     *
     * private final NotificationService notificationService;
     * private final AuditService auditService;
     */

    private final ResourceService resourceService;
    private final InfrastructureService infrastructureService;

    // ✅ MICROservice dependency
    private final UserClient userClient;

    public ResourceRequestServiceImpl(
            ResourceRequestRepository requestRepo,
            ResourceRepository resourceRepo,
            InfrastructureRepository infraRepo,
            ResourceService resourceService,
            InfrastructureService infrastructureService,
            UserClient userClient
            /* NotificationService notificationService */
    ) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.infraRepo = infraRepo;
        this.resourceService = resourceService;
        this.infrastructureService = infrastructureService;
        this.userClient = userClient;
        // this.notificationService = notificationService;

        log.info("ResourceRequestServiceImpl initialized");
    }

    // ----------------------------------------------------
    // ROLE VALIDATION LOGIC (STUDENT → RESOURCE, FACULTY → INFRA)
    // ----------------------------------------------------
    private void validateRole(Long userId, RequestItemType type) {

        UserDTO user = userClient.getUserById(userId);

        if (!user.active()) {
            throw new IllegalStateException("User is inactive");
        }

        if (type == RequestItemType.RESOURCE &&
            !"STUDENT".equalsIgnoreCase(user.role())) {
            throw new RoleMismatchException("Only STUDENT can submit RESOURCE requests.");
        }

        if (type == RequestItemType.INFRASTRUCTURE &&
            !"FACULTY".equalsIgnoreCase(user.role())) {
            throw new RoleMismatchException("Only FACULTY can submit INFRASTRUCTURE requests.");
        }
    }

    // ----------------------------------------------------
    // SUBMIT RESOURCE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest submitResourceRequest(
            Long requesterUserId,
            Long resourceId,
            int quantity) {

        log.info("Submitting Resource Request → requesterId={}, resourceId={}, qty={}",
                requesterUserId, resourceId, quantity);

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be > 0.");
        }

        validateRole(requesterUserId, RequestItemType.RESOURCE);

        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Resource not found: " + resourceId));

        ResourceRequest rr = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .resource(resource)
                .itemType(RequestItemType.RESOURCE)
                .quantity(quantity)
                .status(RequestStatus.SUBMITTED)
                .build();

        ResourceRequest saved = requestRepo.save(rr);

        /*
         * ❌ MONOLITHIC NOTIFICATION (COMMENTED)
         *
         * String message = requester.getName()
         *         + " submitted a RESOURCE request.";
         * notifyProgramManagers(saved.getRequestId(), message);
         */

        return saved;
    }

    // ----------------------------------------------------
    // SUBMIT INFRASTRUCTURE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest submitInfrastructureRequest(
            Long requesterUserId,
            Long infraId) {

        validateRole(requesterUserId, RequestItemType.INFRASTRUCTURE);

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Infrastructure not found: " + infraId));

        ResourceRequest rr = ResourceRequest.builder()
                .requesterUserId(requesterUserId)
                .infrastructure(infra)
                .itemType(RequestItemType.INFRASTRUCTURE)
                .status(RequestStatus.SUBMITTED)
                .build();

        ResourceRequest saved = requestRepo.save(rr);

        /*
         * ❌ MONOLITHIC NOTIFICATION (COMMENTED)
         *
         * String message = requester.getName()
         *         + " submitted an INFRASTRUCTURE request.";
         * notifyProgramManagers(saved.getRequestId(), message);
         */

        return saved;
    }

    // ----------------------------------------------------
    // SEND NOTIFICATION TO PROGRAM MANAGERS (MONOLITHIC)
    // ----------------------------------------------------
    /*
    private void notifyProgramManagers(Long reqId, String message) {

        List<User> managers = userRepo.findByRole(Role.PROG_MANAGER);

        for (User pm : managers) {
            notificationService.createNotification(
                    pm.getUserId(),
                    reqId,
                    message,
                    "REQUEST",
                    pm.getEmail()
            );
        }
    }
    */

    // ----------------------------------------------------
    // APPROVE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest approve(
            Long requestId,
            Long approverUserId) {

        userClient.getUserById(approverUserId);

        ResourceRequest rr = getById(requestId);

        if (rr.getItemType() == RequestItemType.RESOURCE) {
            resourceService.allocate(
                    rr.getResource().getResourceId(),
                    rr.getQuantity());
        } else {
            infrastructureService.markInUse(
                    rr.getInfrastructure().getInfraId());
        }

        rr.setStatus(RequestStatus.APPROVED);
        rr.setApprovedByUserId(approverUserId);
        rr.setDecisionAt(Instant.now());

        /*
         * ❌ MONOLITHIC NOTIFICATION + AUDIT (COMMENTED)
         *
         * notificationService.createNotification(...)
         * auditService.logAction(...)
         */

        return requestRepo.save(rr);
    }

    // ----------------------------------------------------
    // DECLINE REQUEST
    // ----------------------------------------------------
    @Override
    public ResourceRequest decline(
            Long requestId,
            Long approverUserId,
            String reason) {

        userClient.getUserById(approverUserId);

        ResourceRequest rr = getById(requestId);

        rr.setStatus(RequestStatus.DECLINED);
        rr.setApprovedByUserId(approverUserId);
        rr.setDecisionAt(Instant.now());

        /*
         * ❌ MONOLITHIC NOTIFICATION (COMMENTED)
         *
         * notificationService.createNotification(...)
         */

        return requestRepo.save(rr);
    }

    // ----------------------------------------------------
    // LIST / GET
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
                        new EntityNotFoundException(
                                "Request not found"));
    }

    // ----------------------------------------------------
    // MARK IN REVIEW
    // ----------------------------------------------------
    @Override
    public ResourceRequest markInReview(
            Long requestId,
            Long reviewerUserId) {

        userClient.getUserById(reviewerUserId);

        ResourceRequest rr = getById(requestId);

        rr.setStatus(RequestStatus.IN_REVIEW);
        rr.setApprovedByUserId(reviewerUserId);
        rr.setDecisionAt(Instant.now());

        /*
         * ❌ MONOLITHIC AUDIT (COMMENTED)
         *
         * auditService.logAction(
         *     reviewerUserId,
         *     "REQUEST_IN_REVIEW",
         *     requestId
         * );
         */

        return requestRepo.save(rr);
    }
}