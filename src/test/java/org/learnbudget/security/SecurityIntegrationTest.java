package org.learnbudget.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.learnbudget.dto.request.LoginRequest;
import org.learnbudget.dto.request.RegisterRequest;
import org.learnbudget.dto.response.AuthResponse;
import org.learnbudget.repository.UserRepository;
import org.learnbudget.service.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

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
    }

    // ==================== PUBLIC ENDPOINTS TESTS ====================

    @Nested
    @DisplayName("Public Endpoints Tests")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Should allow access to register endpoint without authentication")
        void shouldAllowRegisterWithoutAuth() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should allow access to login endpoint without authentication")
        void shouldAllowLoginWithoutAuth() throws Exception {
            // First register
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)));

            // Then login
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access to refresh endpoint without authentication")
        void shouldAllowRefreshWithoutAuth() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"any-token\"}"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized()); // Invalid token, but endpoint is accessible
        }
    }

    // ==================== PROTECTED ENDPOINTS TESTS ====================

    @Nested
    @DisplayName("Protected Endpoints Tests")
    class ProtectedEndpointsTests {

        @Test
        @DisplayName("Should block access to protected endpoints without JWT")
        void shouldBlockProtectedEndpointsWithoutJwt() throws Exception {
            mockMvc.perform(get("/api/users/1"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should block access with invalid JWT")
        void shouldBlockWithInvalidJwt() throws Exception {
            mockMvc.perform(get("/api/users/1")
                            .header("Authorization", "Bearer invalid-token"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should block access with malformed Authorization header")
        void shouldBlockWithMalformedHeader() throws Exception {
            mockMvc.perform(get("/api/users/1")
                            .header("Authorization", "InvalidFormat token"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should allow access to protected endpoints with valid JWT")
        void shouldAllowAccessWithValidJwt() throws Exception {
            // Register and login to get valid token
            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Get user ID from registration response
            Long userId = objectMapper.readTree(responseBody).get("id").asLong();

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andReturn();

            String loginResponseBody = loginResult.getResponse().getContentAsString();
            AuthResponse authResponse = objectMapper.readValue(loginResponseBody, AuthResponse.class);
            String accessToken = authResponse.getAccessToken();

            // Access protected endpoint with valid token
            mockMvc.perform(get("/api/users/" + userId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"));
        }
    }

    // ==================== JWT AUTHENTICATION FLOW TESTS ====================

    @Nested
    @DisplayName("JWT Authentication Flow Tests")
    class JwtAuthenticationFlowTests {

        @Test
        @DisplayName("Should complete full authentication flow")
        void shouldCompleteFullAuthFlow() throws Exception {
            // 1. Register
            MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String registerBody = registerResult.getResponse().getContentAsString();
            Long userId = objectMapper.readTree(registerBody).get("id").asLong();

            // 2. Login
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginBody = loginResult.getResponse().getContentAsString();
            AuthResponse authResponse = objectMapper.readValue(loginBody, AuthResponse.class);

            assertThat(authResponse.getAccessToken()).isNotEmpty();
            assertThat(authResponse.getRefreshToken()).isNotEmpty();

            // 3. Access protected resource with access token
            mockMvc.perform(get("/api/users/" + userId)
                            .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"));

            // 4. Get current user
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"));
        }

        @Test
        @DisplayName("Should accept valid refresh token and return new tokens")
        void shouldAcceptValidRefreshToken() throws Exception {
            // Register and login
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)));

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andReturn();

            String loginBody = loginResult.getResponse().getContentAsString();
            AuthResponse authResponse = objectMapper.readValue(loginBody, AuthResponse.class);
            String refreshToken = authResponse.getRefreshToken();

            // Use refresh token to get new tokens
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
        }

        @Test
        @DisplayName("Should generate new tokens on refresh (different from original)")
        void shouldGenerateNewTokensOnRefresh() throws Exception {
            // Register and login
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)));

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andReturn();

            String loginBody = loginResult.getResponse().getContentAsString();
            AuthResponse authResponse = objectMapper.readValue(loginBody, AuthResponse.class);
            String refreshToken = authResponse.getRefreshToken();

            // Wait to ensure different timestamp
            Thread.sleep(1100);

            // Use refresh token to get new tokens
            MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            String refreshBody = refreshResult.getResponse().getContentAsString();
            AuthResponse newAuthResponse = objectMapper.readValue(refreshBody, AuthResponse.class);

            // After waiting, tokens should be different
            assertThat(newAuthResponse.getAccessToken()).isNotEqualTo(authResponse.getAccessToken());
        }
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should accept access token from different user")
        void shouldAcceptTokenFromDifferentUser() throws Exception {
            // Register first user
            RegisterRequest user1Request = RegisterRequest.builder()
                    .email("user1@example.com")
                    .password("password123")
                    .firstName("User")
                    .lastName("One")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user1Request)));

            // Register second user
            RegisterRequest user2Request = RegisterRequest.builder()
                    .email("user2@example.com")
                    .password("password123")
                    .firstName("User")
                    .lastName("Two")
                    .build();

            MvcResult user2Register = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user2Request)))
                    .andReturn();

            String user2Body = user2Register.getResponse().getContentAsString();
            Long user2Id = objectMapper.readTree(user2Body).get("id").asLong();

            // Login as user1
            LoginRequest user1Login = LoginRequest.builder()
                    .email("user1@example.com")
                    .password("password123")
                    .build();

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user1Login)))
                    .andReturn();

            String loginBody = loginResult.getResponse().getContentAsString();
            AuthResponse authResponse = objectMapper.readValue(loginBody, AuthResponse.class);

            // User1 tries to access User2's data - should work (get data)
            mockMvc.perform(get("/api/users/" + user2Id)
                            .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user2@example.com"));

            // But User1 cannot update User2's profile
            mockMvc.perform(put("/api/users/" + user2Id)
                            .header("Authorization", "Bearer " + authResponse.getAccessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Hacked\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}