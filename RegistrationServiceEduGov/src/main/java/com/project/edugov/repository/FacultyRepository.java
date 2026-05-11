package com.project.edugov.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.edugov.model.Faculty;
import com.project.edugov.model.Status;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    // 🟢 Keep only the Long versions!
    Optional<Faculty> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId); 
    
    List<Faculty> findByStatus(Status status);
    
    List<Faculty> findByDepartmentIgnoreCase(String department);
}