package org.learnbudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.learnbudget.dto.request.LoginRequest;
import org.learnbudget.dto.request.RefreshTokenRequest;
import org.learnbudget.dto.request.RegisterRequest;
import org.learnbudget.dto.response.AuthResponse;
import org.learnbudget.dto.response.UserResponse;
import org.learnbudget.exception.DuplicateEmailException;
import org.learnbudget.exception.InvalidCredentialsException;
import org.learnbudget.security.CustomUserDetailsService;
import org.learnbudget.service.JWTService;
import org.learnbudget.service.UserService;
import org.learnbudget.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean  // ✅ Add this
    private JWTService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean  // ✅ Add this
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        loginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("jwt-access-token")
                .refreshToken("jwt-refresh-token")
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    // ==================== REGISTRATION TESTS ====================

    @Nested
    @DisplayName("Registration Endpoint Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully and return 201 Created")
        void shouldRegisterUserSuccessfully() throws Exception {
            // Arrange
            when(userService.register(any(RegisterRequest.class))).thenReturn(userResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")))
                    .andExpect(jsonPath("$.createdAt", notNullValue()));

            verify(userService, times(1)).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when email is missing")
        void shouldReturnBadRequestWhenEmailMissing() throws Exception {
            // Arrange
            registerRequest.setEmail(null);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(userService, never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturnBadRequestWhenPasswordTooShort() throws Exception {
            // Arrange
            registerRequest.setPassword("short");

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(userService, never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict when email already exists")
        void shouldReturnConflictWhenEmailExists() throws Exception {
            // Arrange
            when(userService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateEmailException("Email already registered"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(userService, times(1)).register(any(RegisterRequest.class));
        }
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully and return tokens")
        void shouldLoginSuccessfully() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken", is("jwt-access-token")))
                    .andExpect(jsonPath("$.refreshToken", is("jwt-refresh-token")))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.user.id", is(1)))
                    .andExpect(jsonPath("$.user.email", is("john.doe@example.com")));

            verify(userService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void shouldReturnUnauthorizedWhenCredentialsInvalid() throws Exception {
            // Arrange
            when(userService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(userService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when email is missing")
        void shouldReturnBadRequestWhenEmailMissing() throws Exception {
            // Arrange
            loginRequest.setEmail(null);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(userService, never()).login(any(LoginRequest.class));
        }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Refresh Token Endpoint Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh tokens successfully")
        void shouldRefreshTokensSuccessfully() throws Exception {
            // Arrange
            when(userService.refreshToken(anyString())).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken", is("jwt-access-token")))
                    .andExpect(jsonPath("$.refreshToken", is("jwt-refresh-token")))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.user", notNullValue()));

            verify(userService, times(1)).refreshToken("valid-refresh-token");
        }

        @Test
        @DisplayName("Should return 401 when refresh token is invalid")
        void shouldReturnUnauthorizedWhenRefreshTokenInvalid() throws Exception {
            // Arrange
            when(userService.refreshToken(anyString()))
                    .thenThrow(new InvalidCredentialsException("Invalid refresh token"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(userService, times(1)).refreshToken(anyString());
        }

        @Test
        @DisplayName("Should return 400 when refresh token is missing")
        void shouldReturnBadRequestWhenRefreshTokenMissing() throws Exception {
            // Arrange
            refreshTokenRequest.setRefreshToken(null);

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(userService, never()).refreshToken(anyString());
        }

        @Test
        @DisplayName("Should return 400 when refresh token is empty")
        void shouldReturnBadRequestWhenRefreshTokenEmpty() throws Exception {
            // Arrange
            refreshTokenRequest.setRefreshToken("");

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(userService, never()).refreshToken(anyString());
        }
    }
}
