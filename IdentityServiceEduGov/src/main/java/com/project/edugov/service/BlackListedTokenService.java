package com.project.edugov.service;

public interface BlackListedTokenService {
	void addToBlacklist(String token);
    boolean isBlacklisted(String token);
}
