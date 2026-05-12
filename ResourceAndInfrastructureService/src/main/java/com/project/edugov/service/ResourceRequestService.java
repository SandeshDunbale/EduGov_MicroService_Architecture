package com.project.edugov.service;

import java.util.List;

import com.project.edugov.model.RequestStatus;
import com.project.edugov.model.ResourceRequest;

public interface ResourceRequestService {

    ResourceRequest submitResourceRequest(
            Long requesterUserId,
            Long resourceId,
            int quantity
    );

    ResourceRequest submitInfrastructureRequest(
            Long requesterUserId,
            Long infraId
    );

    ResourceRequest markInReview(
            Long requestId,
            Long reviewerUserId
    );

    ResourceRequest approve(
            Long requestId,
            Long approverUserId
    );

    ResourceRequest decline(
            Long requestId,
            Long approverUserId,
            String reasonOptional
    );

    List<Object> listByStatus(RequestStatus status);

    List<Object> listByRequester(Long requesterUserId);

    ResourceRequest getById(Long requestId);
}