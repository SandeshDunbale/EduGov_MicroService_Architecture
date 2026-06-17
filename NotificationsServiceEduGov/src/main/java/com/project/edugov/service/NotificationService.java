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
 
@Service
public class NotificationService {
 
    @Autowired
    private NotificationRepository repository;

    // Inject the mail sender
    @Autowired
    private JavaMailSender mailSender;
 
    public Notification createNotification(Long userId, Long entityId,
                                           String message, String category,
                                           String email) {
 
        // 1. Save to Database
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEntityId(entityId);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setStatus("UNREAD");
        notification.setCreatedDate(LocalDateTime.now());
        
        Notification savedNotification = repository.save(notification);

        // 2. Send the Email
        sendEmailAlert(email, category, message);
 
        return savedNotification;
    }

    private void sendEmailAlert(String toEmail, String category, String messageBody) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            // This should match the spring.mail.username in your properties
            mailMessage.setFrom("mrunalip263@gmail.com"); 
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("EduGov Alert: " + category);
            mailMessage.setText(messageBody);

            mailSender.send(mailMessage);
            System.out.println("Email successfully sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            // You might want to log this properly or handle the exception 
            // depending on if email failure should roll back the DB save.
        }
    }
 
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