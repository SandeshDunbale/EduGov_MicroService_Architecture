package com.project.edugov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.edugov.model.BlackListedToken;

@Repository
public interface BlackListedTokenRepository extends JpaRepository<BlackListedToken, Long> {
    boolean existsByToken(String token);
}