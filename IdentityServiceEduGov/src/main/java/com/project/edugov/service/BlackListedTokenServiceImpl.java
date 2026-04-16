package com.project.edugov.service;


import org.springframework.stereotype.Service;

import com.project.edugov.model.BlackListedToken;
import com.project.edugov.repository.BlackListedTokenRepository;

@Service
public class BlackListedTokenServiceImpl implements BlackListedTokenService{

    private final BlackListedTokenRepository repository;

    public BlackListedTokenServiceImpl(BlackListedTokenRepository repository) {
        this.repository = repository;
    }

    public void addToBlacklist(String token) {
        // Prevent adding the same token twice just in case they click logout twice
        if (!repository.existsByToken(token)) {
            BlackListedToken blacklistedToken = new BlackListedToken();
            blacklistedToken.setToken(token);
            repository.save(blacklistedToken);
        }
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByToken(token);
    }
}