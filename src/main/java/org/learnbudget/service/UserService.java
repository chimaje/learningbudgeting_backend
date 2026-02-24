package org.learnbudget.service;


import org.learnbudget.dto.request.LoginRequest;
import org.learnbudget.dto.request.RegisterRequest;
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
     * @throws DuplicateEmailException if email already exists
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticate user and generate token
     * @param request login credentials
     * @return authentication response with JWT token
     * @throws InvalidCredentialsException if credentials are invalid
     */
    AuthResponse login(LoginRequest request);

    /**
     * Find user by email
     * @param email user's email
     * @return user response
     * @throws UserNotFoundException if user not found
     */
    UserResponse findByEmail(String email);

    /**
     * Find user by ID
     * @param id user's ID
     * @return user response
     * @throws UserNotFoundException if user not found
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
     * @throws UserNotFoundException if user not found
     */
    void deleteById(Long id);
}
