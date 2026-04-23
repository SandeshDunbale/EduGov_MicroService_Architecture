package com.project.edugov.service;

import java.util.List;

import com.project.edugov.model.Resource;
import com.project.edugov.model.ResourceStatus;
import com.project.edugov.model.ResourceType;

public interface ResourceService {

    Resource create(Long programId, ResourceType type, Integer quantity, ResourceStatus status);

    Resource getById(Long resourceId);

    List<Resource> findByProgramId(Long programId);

    List<Resource> findByStatus(ResourceStatus status);

    Resource updateStatus(Long resourceId, ResourceStatus status);

    Resource update(Long id, Long programId, ResourceType type, Integer qty, ResourceStatus status);

    Resource allocate(Long resourceId, int qtyToAllocate);

    List<Resource> findAll();

    void delete(Long resourceId);
}