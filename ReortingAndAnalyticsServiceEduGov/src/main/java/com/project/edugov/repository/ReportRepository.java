package com.project.edugov.repository;
 
import com.project.edugov.model.Report;
import com.project.edugov.model.ReportScope;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.time.LocalDateTime;
import java.util.List;
 
public interface ReportRepository extends JpaRepository<Report, Long> {
 
    List<Report> findByScope(ReportScope scope);
 
    List<Report> findByGeneratedDateBetween(LocalDateTime start, LocalDateTime end);
}