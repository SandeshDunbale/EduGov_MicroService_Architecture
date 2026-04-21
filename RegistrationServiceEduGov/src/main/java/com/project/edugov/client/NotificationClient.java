package com.project.edugov.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// name: Teammate ki notification service ka jo application name hai (check in Eureka)
@FeignClient(name = "NotificationServiceEduGov") 
public interface NotificationClient {

    @PostMapping("/api/notifications/create")
    void createNotification(
        @RequestParam Long senderId,
        @RequestParam Long receiverId,
        @RequestParam String message,
        @RequestParam String type,
        @RequestParam String senderName
    );
}
