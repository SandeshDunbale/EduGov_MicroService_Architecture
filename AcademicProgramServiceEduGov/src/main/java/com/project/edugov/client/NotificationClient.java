package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// The name must match the spring.application.name of your Notification Service in Eureka
@FeignClient(name = "NOTIFICATIONSSERVICEEDUGOV")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    void sendNotification(
            @RequestParam("userId") Long userId,
            @RequestParam("entityId") Long entityId,
            @RequestParam("message") String message,
            @RequestParam("category") String category,
            @RequestParam("email") String email
    );
}