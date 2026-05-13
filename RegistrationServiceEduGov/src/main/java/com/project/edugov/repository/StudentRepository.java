package com.project.edugov.repository;

import com.project.edugov.model.Student;
import com.project.edugov.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    
    List<Student> findByStatus(Status status);

    /**
     * Finds a student profile by the userId linked from the IAM microservice.
     */
    Optional<Student> findByUserId(Long userId);

    /**
     * Checks if a profile already exists for a specific user ID.
     */
    boolean existsByUserId(Long userId);
    

}