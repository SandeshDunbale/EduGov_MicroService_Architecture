package com.project.edugov.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.project.edugov.exception.ResourceNotFoundException;
import com.project.edugov.model.Notification;
import com.project.edugov.repository.NotificationRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Main method to create notification. 
     * It saves to DB first, then attempts a protected email call.
     */
    public Notification createNotification(Long userId, Long entityId,
            String message, String category,
            String email) {

// 1. Save to Database (This works regardless of email)
Notification notification = new Notification();
notification.setUserId(userId);
notification.setEntityId(entityId);
notification.setMessage(message);
notification.setCategory(category);
notification.setStatus("UNREAD");
notification.setCreatedDate(LocalDateTime.now());

Notification savedNotification = repository.save(notification);

// 2. Wrap in Throwable to catch the Circuit Breaker "Open" exception
try {
sendEmailWithCircuitBreaker(email, category, message);
} catch (Throwable t) { 
// This catch block intercepts the 503 'CallNotPermittedException'
log.error("Email skipped! Circuit is OPEN or SMTP failed. Notification saved to DB. Error: {}", t.getMessage());
}

// Now this return WILL be reached, giving you a 200 OK with the data
return savedNotification; 
}

    /**
     * This method is monitored by Resilience4j.
     * If mailSender.send throws an exception, the fallbackMethod is called.
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "fallbackEmail")
    public void sendEmailWithCircuitBreaker(String toEmail, String category, String messageBody) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("mrunalip263@gmail.com"); 
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("EduGov Alert: " + category);
        mailMessage.setText(messageBody);

        mailSender.send(mailMessage);
        log.info("Email successfully sent to: {}", toEmail);
    }

    /**
     * FALLBACK METHOD
     * Must have the same parameters as the original method + Throwable.
     */
    public void fallbackEmail(String toEmail, String category, String messageBody, Throwable t) {
        log.error("CRITICAL: Email service failed for {}. Circuit Breaker active. Error: {}", 
                  toEmail, t.getMessage());
        
        // Logic: Since email failed, we just log it. 
        // The notification is already safe in the Database from step 1.
    }

    // --- Standard CRUD Methods ---

    public List<Notification> getAll() {
        List<Notification> list = repository.findAll();
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("No notifications found");
        }
        return list;
    }

    public List<Notification> getByUser(Long userId) {
        List<Notification> list = repository.findByUserId(userId);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("No notifications for user");
        }
        return list;
    }

    public List<Notification> getByCategory(String category) {
        List<Notification> list = repository.findByCategory(category);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("No notifications for category");
        }
        return list;
    }

    public Notification markAsRead(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setStatus("READ");
        return repository.save(notification);
    }
}