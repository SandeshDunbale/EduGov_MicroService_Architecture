package com.project.edugov.controller;
 
import com.project.edugov.model.Notification;
import com.project.edugov.service.NotificationService;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
 
    @Autowired
    private NotificationService service;
 
    // CREATE
    @PostMapping
    public Notification createNotification(
            @RequestParam Long userId,
            @RequestParam Long entityId,
            @RequestParam String message,
            @RequestParam String category,
            @RequestParam String email) {
 
        return service.createNotification(userId, entityId, message, category, email);
    }
 
    // GET ALL
    @GetMapping
    public List<Notification> getAll() {
        return service.getAll();
    }
 
    // GET BY USER
    @GetMapping("/user/{userId}")
    public List<Notification> getByUser(@PathVariable Long userId) {
        return service.getByUser(userId);
    }
 
    //  FIXED (IMPORTANT)
    @GetMapping("/category/{category}")
    public List<Notification> getByCategory(@PathVariable String category) {
        return service.getByCategory(category);
    }
 
    // MARK AS READ
    @PostMapping("/read/{id}")
    public Notification markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }
}