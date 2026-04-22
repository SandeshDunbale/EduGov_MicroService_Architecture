package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

// The name MUST match 'ResearchAndGrantServiceEduGov' from your properties
@FeignClient(name = "ResearchAndGrantServiceEduGov", contextId = "grantClient")
public interface GrantClient {

    @GetMapping("/api/grants/count")
    long getTotalGrants();

    @GetMapping("/api/grants/sum")
    Double getTotalGrantAmount();
}