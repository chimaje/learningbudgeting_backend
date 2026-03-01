package org.learnbudget.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.learnbudget.dto.request.LoginRequest;
import org.learnbudget.dto.request.RegisterRequest;
import org.learnbudget.dto.request.UpdateUserRequest;
import org.learnbudget.dto.response.AuthResponse;
import org.learnbudget.dto.response.UserResponse;
import org.learnbudget.exception.DuplicateEmailException;
import org.learnbudget.exception.InvalidCredentialsException;
import org.learnbudget.exception.UnauthorizedException;
import org.learnbudget.exception.UserNotFoundException;
import org.learnbudget.model.User;
import org.learnbudget.repository.UserRepository;
import org.learnbudget.service.JWTService;
import org.learnbudget.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    @Override
    public UserResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            log.warn("Registration failed: Email already exists - {}", request.getEmail());
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail().toLowerCase()) // Normalize email to lowercase
                .password(passwordEncoder.encode(request.getPassword())) // Hash password
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid password for user - {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getEmail());

        String accesstoken = jwtService.generateAccessToken(user);
        String refreshtoken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accesstoken)
                .refreshToken(refreshtoken)
                .type("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }
    @Override
    public AuthResponse refreshToken(String refreshToken) {

        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            log.warn("Token refresh failed: Invalid refresh token");
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        log.info("Token refreshed successfully for user: {}", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .type("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        log.debug("Finding user by email: {}", email);

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        log.debug("Finding user by ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        log.debug("Finding all users");

        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.toLowerCase());
    }

    @Override
    public void deleteById(Long id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }
    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, String authenticatedUserEmail) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // Check if user is updating their own profile
        if (!user.getEmail().equals(authenticatedUserEmail.toLowerCase())) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        // Update fields if provided
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", id);

        return mapToUserResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long id, String authenticatedUserEmail) {
        log.info("Deleting user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // Check if user is deleting their own profile
        if (!user.getEmail().equals(authenticatedUserEmail.toLowerCase())) {
            throw new UnauthorizedException("You can only delete your own profile");
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }
    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}