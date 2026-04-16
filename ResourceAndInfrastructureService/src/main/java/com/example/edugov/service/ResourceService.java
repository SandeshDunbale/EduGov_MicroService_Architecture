package com.example.edugov.service;

import com.example.edugov.model.Resource;
import com.example.edugov.model.ResourceStatus;
import com.example.edugov.model.ResourceType;

import java.util.List;

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