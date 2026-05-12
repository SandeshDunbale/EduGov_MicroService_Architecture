package com.project.edugov.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.edugov.dto.InfrastructureRequestResponse;
import com.project.edugov.dto.ProgramDTO;
import com.project.edugov.dto.ResourceRequestResponse;
import com.project.edugov.dto.UserDTO;
import com.project.edugov.exception.DownstreamServiceUnavailableException;
import com.project.edugov.exception.RoleMismatchException;
import com.project.edugov.feign.NotificationClient;
import com.project.edugov.feign.ProgramClient;
import com.project.edugov.feign.UserClient;
import com.project.edugov.model.Infrastructure;
import com.project.edugov.model.RequestItemType;
import com.project.edugov.model.RequestStatus;
import com.project.edugov.model.Resource;
import com.project.edugov.model.ResourceRequest;
import com.project.edugov.repository.InfrastructureRepository;
import com.project.edugov.repository.ResourceRepository;
import com.project.edugov.repository.ResourceRequestRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

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
    private final NotificationClient notificationClient;

    private final AsyncAuditLogger auditLogger;
    private final ProgramClient programClient;

    public ResourceRequestServiceImpl(
            ResourceRequestRepository requestRepo,
            ResourceRepository resourceRepo,
            InfrastructureRepository infraRepo,
            ResourceService resourceService,
            InfrastructureService infrastructureService,
            UserClient userClient,
            NotificationClient notificationClient,
            AsyncAuditLogger auditLogger,
            ProgramClient programClient
    ) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.infraRepo = infraRepo;
        this.resourceService = resourceService;
        this.infrastructureService = infrastructureService;
        this.userClient = userClient;
        this.notificationClient = notificationClient;
        this.auditLogger = auditLogger;
        this.programClient = programClient;

        log.info("✅ ResourceRequestServiceImpl initialized");
    }

    // ====================================================
    // ✅ USER SERVICE
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
    // ✅ NOTIFICATION SERVICE
    // ====================================================
    @CircuitBreaker(name = "notificationService", fallbackMethod = "notificationFallback")
    private void notifyUser(Long userId, Long entityId, String message, String category, String email) {
        notificationClient.sendNotification(userId, entityId, message, category, email);
    }

    private void notificationFallback(
            Long userId, Long entityId, String message,
            String category, String email, Throwable ex
    ) {
        log.warn("Notification skipped. Service down for userId={}", userId);
    }

    // ====================================================
    // ✅ ROLE VALIDATION
    // ====================================================
    private void validateRole(Long userId, RequestItemType type) {

        UserDTO user = fetchUser(userId);

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

    // ====================================================
    // ✅ SUBMIT RESOURCE REQUEST
    // ====================================================
    @Override
    public ResourceRequest submitResourceRequest(Long requesterUserId, Long resourceId, int quantity) {

        log.info("Submitting Resource Request → requesterId={}, resourceId={}, qty={}",
                requesterUserId, resourceId, quantity);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        validateRole(requesterUserId, RequestItemType.RESOURCE);

        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found: " + resourceId));

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

        auditLogger.fireAndForgetLog(
                requesterUserId,
                "SUBMIT_RESOURCE_REQUEST",
                "Resource ID: " + resourceId + ", Qty: " + quantity
        );

        return saved;
    }

    // ====================================================
    // ✅ SUBMIT INFRA REQUEST
    // ====================================================
    @Override
    public ResourceRequest submitInfrastructureRequest(Long requesterUserId, Long infraId) {

        validateRole(requesterUserId, RequestItemType.INFRASTRUCTURE);

        Infrastructure infra = infraRepo.findById(infraId)
                .orElseThrow(() -> new EntityNotFoundException("Infrastructure not found: " + infraId));

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

        auditLogger.fireAndForgetLog(
                requesterUserId,
                "SUBMIT_INFRASTRUCTURE_REQUEST",
                "Infra ID: " + infraId
        );

        return saved;
    }

    // ====================================================
    // ✅ APPROVE
    // ====================================================
    @Override
    public ResourceRequest approve(Long requestId, Long approverUserId) {

        fetchUser(approverUserId);

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

        auditLogger.fireAndForgetLog(
                approverUserId,
                "APPROVE_REQUEST",
                "Request ID: " + requestId
        );

        return saved;
    }

    // ====================================================
    // ✅ DECLINE
    // ====================================================
    @Override
    public ResourceRequest decline(Long requestId, Long approverUserId, String reason) {

        fetchUser(approverUserId);

        ResourceRequest request = getById(requestId);

        request.setStatus(RequestStatus.DECLINED);
        request.setApprovedByUserId(approverUserId);
        request.setDecisionAt(Instant.now());
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

        auditLogger.fireAndForgetLog(
                approverUserId,
                "DECLINE_REQUEST",
                "Request ID: " + requestId + ", Reason: " + reason
        );

        return saved;
    }

    // ====================================================
    // ✅ READ OPERATIONS
    // ====================================================
    @Override
    @Transactional(readOnly = true)
    public List<Object> listByStatus(RequestStatus status) {

        List<ResourceRequest> requests = requestRepo.findByStatus(status);

        Map<Long, String> programMap = getProgramMap();

        return requests.stream().map(req -> {

            if (req.getItemType() == RequestItemType.RESOURCE) {

                Resource res = req.getResource();

                return ResourceRequestResponse.builder()
                        .requestId(req.getRequestId())
                        .requesterUserId(req.getRequesterUserId())
                        .itemType(req.getItemType())
                        .status(req.getStatus())
                        .resourceId(res != null ? res.getResourceId() : null)
                        .quantity(req.getQuantity())
                        .createdAt(req.getCreatedAt())
                        .reason(req.getReason())
                        .resourceType(res != null ? res.getType().name() : null)

                        // ✅ FIX
                        .programName(programMap.getOrDefault(
                                res != null ? res.getProgramId() : null,
                                "Unknown Program"
                        ))

                        .build();

            } else {

                Infrastructure infra = req.getInfrastructure();

                return InfrastructureRequestResponse.builder()
                        .requestId(req.getRequestId())
                        .requesterUserId(req.getRequesterUserId())
                        .itemType(req.getItemType())
                        .status(req.getStatus())
                        .infraId(infra != null ? infra.getInfraId() : null)
                        .infraCapacity(infra != null ? infra.getCapacity() : null)

                        // ✅ ADD LOCATION
                        .location(infra != null ? infra.getLocation() : null)

                        .createdAt(req.getCreatedAt())
                        .reason(req.getReason())
                        .infrastructureType(infra != null ? infra.getType().name() : null)

                        // ✅ FIX
                        .programName(programMap.getOrDefault(
                                infra != null ? infra.getProgramId() : null,
                                "Unknown Program"
                        ))

                        .build();
            }

        }).toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<Object> listByRequester(Long requesterUserId) {

        List<ResourceRequest> requests = requestRepo.findByRequesterUserId(requesterUserId);

        // ✅ Only ONE Feign call
        Map<Long, String> programMap = getProgramMap();

        return requests.stream().map(req -> {

            if (req.getItemType() == RequestItemType.RESOURCE) {

                Resource res = req.getResource();

                return ResourceRequestResponse.builder()
                        .requestId(req.getRequestId())
                        .requesterUserId(req.getRequesterUserId())
                        .itemType(req.getItemType())
                        .status(req.getStatus())
                        .resourceId(res != null ? res.getResourceId() : null)
                        .quantity(req.getQuantity())
                        .createdAt(req.getCreatedAt())
                        .reason(req.getReason())
                        .resourceType(res != null ? res.getType().name() : null)

                        // ✅ Optimized usage
                        .programName(programMap.getOrDefault(
                                res != null ? res.getProgramId() : null,
                                "Unknown Program"
                        ))

                        .build();

            } else {

                Infrastructure infra = req.getInfrastructure();

                return InfrastructureRequestResponse.builder()
                        .requestId(req.getRequestId())
                        .requesterUserId(req.getRequesterUserId())
                        .itemType(req.getItemType())
                        .status(req.getStatus())
                        .infraId(infra != null ? infra.getInfraId() : null)
                        .infraCapacity(infra != null ? infra.getCapacity() : null)
                        .createdAt(req.getCreatedAt())
                        .reason(req.getReason())
                        .infrastructureType(infra != null ? infra.getType().name() : null)

                        // ✅ Optimized
                        .programName(programMap.getOrDefault(
                                infra != null ? infra.getProgramId() : null,
                                "Unknown Program"
                        ))

                        .build();
            }

        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceRequest getById(Long requestId) {
        return requestRepo.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));
    }

    // ====================================================
    // ✅ MARK IN REVIEW
    // ====================================================
    @Override
    public ResourceRequest markInReview(Long requestId, Long reviewerUserId) {

        fetchUser(reviewerUserId);

        ResourceRequest request = getById(requestId);

        request.setStatus(RequestStatus.IN_REVIEW);
        request.setApprovedByUserId(reviewerUserId);
        request.setDecisionAt(Instant.now());

        ResourceRequest saved = requestRepo.save(request);

        auditLogger.fireAndForgetLog(
                reviewerUserId,
                "REQUEST_IN_REVIEW",
                "Request ID: " + requestId
        );

        return saved;
    }
    private String getProgramName(Long programId) {
        try {
            return programClient.getProgramById(programId).title();
        } catch (Exception e) {
            log.warn("Program fetch failed for ID: {}", programId);
            return "Unknown Program";
        }
    }
    private Map<Long, String> getProgramMap() {
        try {
            return programClient.getAllPrograms().stream()
                    .collect(Collectors.toMap(
                            ProgramDTO::programId,
                            ProgramDTO::title   // ✅ or name() if your DTO uses name
                    ));
        } catch (Exception e) {
            log.warn("Program service failed, using fallback");

            return Map.of(
                    1L, "BA",
                    2L, "BSc",
                    3L, "B.Tech"
            );
        }
    }
}