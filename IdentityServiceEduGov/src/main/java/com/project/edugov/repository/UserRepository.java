package com.project.edugov.repository;

import com.project.edugov.model.User;
import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByStatus(Status status);
    Optional<User> findByEmailAndStatus(String email, Status status);
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
}