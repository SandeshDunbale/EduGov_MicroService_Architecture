package com.project.edugov.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "NOTIFICATIONSSERVICEEDUGOV",
    path = "/api/notifications"
)
public interface NotificationClient {

    @PostMapping
    void createNotification(
            @RequestParam Long userId,
            @RequestParam Long entityId,
            @RequestParam String message,
            @RequestParam String category,
            @RequestParam String email
    );
}