package com.project.edugov.service;

import com.project.edugov.model.Role;
import com.project.edugov.model.Status;
import com.project.edugov.model.User;

import java.util.List;
import java.util.Optional;
import com.project.edugov.controller.IdentityController.UserCreateRequest;
public interface UserService {
	// Add this import at the top
	;

	// Add this method to the interface
	User registerUser(UserCreateRequest request);
    /**
     * Authenticates a user based on their email and raw password.
     */
    User authenticate(String email, String rawPassword);

    /**
     * Updates the password for a specific user.
     */
    void updatePassword(String email, String newRawPassword);

    /**
     * Retrieves a list of users by their role.
     */
    List<User> getUserByRole(Role role);

    /**
     * Retrieves a list of users by their current status.
     */
    List<User> getUserByStatus(Status status);

    /**
     * Retrieves a user by their unique ID.
     */
    Optional<User> getUserById(Long userId);

    /**
     * Recovers a user's email address using their phone number.
     */
    String recoverEmailByPhone(String phone);

    /**
     * Updates the status of an existing user.
     */
    User updateUserStatus(Long userId, Status newStatus);
}