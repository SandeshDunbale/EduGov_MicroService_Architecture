package com.project.edugov.repository;
 
import com.project.edugov.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
 
public interface NotificationRepository extends JpaRepository<Notification, Long> {
 
    List<Notification> findByUserId(Long userId);
 
    List<Notification> findByCategory(String category);
}