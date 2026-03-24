package org.learnbudget.service;


import org.learnbudget.dto.request.LoginRequest;
import org.learnbudget.dto.request.RegisterRequest;
import org.learnbudget.dto.request.UpdateUserRequest;
import org.learnbudget.dto.response.AuthResponse;
import org.learnbudget.dto.response.UserResponse;
import org.learnbudget.exception.DuplicateEmailException;
import org.learnbudget.exception.InvalidCredentialsException;
import org.learnbudget.exception.UserNotFoundException;

import java.util.List;

public interface UserService {

    /**
     * Register a new user
     * @param request registration details
     * @return user response with details
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticate user and generate token
     * @param request login credentials
     * @return authentication response with JWT token
     */
    AuthResponse login(LoginRequest request);
    /**
     * Refresh access token using refresh token
     * @param refreshToken the refresh token
     * @return new auth response with fresh tokens
     */
    AuthResponse refreshToken(String refreshToken);
    /**
     * Find user by email
     * @param email user's email
     */
    UserResponse findByEmail(String email);

    /**
     * Find user by ID
     * @param id user's ID
     */
    UserResponse findById(Long id);

    /**
     * Get all users
     * @return list of all users
     */
    List<UserResponse> findAll();

    /**
     * Check if email exists
     * @param email email to check
     * @return true if exists, false otherwise
     */
    boolean emailExists(String email);

    /**
     * Delete user by ID
     * @param id user's ID
     *
     */
    void deleteById(Long id);
    /**
     * Update user information
     */
    UserResponse updateUser(Long id, UpdateUserRequest request, String authenticatedUserEmail);

    /**
     * Delete user (only self or admin)
     */
    void deleteUser(Long id, String authenticatedUserEmail);
}
