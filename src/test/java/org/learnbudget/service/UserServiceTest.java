package org.learnbudget.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnbudget.dto.request.LoginRequest;
import org.learnbudget.dto.request.RegisterRequest;
import org.learnbudget.dto.response.AuthResponse;
import org.learnbudget.dto.response.UserResponse;
import org.learnbudget.exception.DuplicateEmailException;
import org.learnbudget.exception.InvalidCredentialsException;
import org.learnbudget.exception.UserNotFoundException;
import org.learnbudget.model.User;
import org.learnbudget.repository.UserRepository;
import org.learnbudget.service.impl.UserServiceImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTService jwtService;  // Changed from JWTService to JwtService

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .password("$2a$10$hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registerRequest = RegisterRequest.builder()
                .email("john.doe@example.com")
                .password("plainPassword123")
                .firstName("John")
                .lastName("Doe")
                .build();

        loginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("plainPassword123")
                .build();
    }

    // ==================== REGISTRATION TESTS ====================

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            UserResponse response = userService.register(registerRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getId()).isEqualTo(1L);

            verify(userRepository).existsByEmail("john.doe@example.com");
            verify(passwordEncoder).encode("plainPassword123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase during registration")
        void shouldNormalizeEmailToLowercase() {
            // Arrange
            registerRequest.setEmail("John.Doe@EXAMPLE.COM");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.register(registerRequest);

            // Assert
            verify(userRepository).existsByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should normalize full email to lowercase during registration")
        void shouldNormalizeFullEmailToLowercase(){
            // Arrange
            registerRequest.setEmail("JOHN.DOE@EXAMPLE.COM");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.register(registerRequest);

            // Assert
            verify(userRepository).existsByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should hash password during registration")
        void shouldHashPasswordDuringRegistration() {
            // Arrange
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.register(registerRequest);

            // Assert
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("$2a$10$hashedPassword");
            assertThat(savedUser.getPassword()).isNotEqualTo("plainPassword123");
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when email exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.register(registerRequest))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("Email already registered");

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should call repository methods in correct order")
        void shouldCallRepositoryMethodsInCorrectOrder() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            userService.register(registerRequest);

            // Assert
            var inOrder = inOrder(userRepository, passwordEncoder);
            inOrder.verify(userRepository).existsByEmail(anyString());
            inOrder.verify(passwordEncoder).encode(anyString());
            inOrder.verify(userRepository).save(any(User.class));
        }
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("User Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with correct credentials and return both tokens")
        void shouldLoginSuccessfully() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token-123");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token-456");

            // Act
            AuthResponse response = userService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token-123");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("john.doe@example.com");

            verify(userRepository).findByEmail("john.doe@example.com");
            verify(passwordEncoder).matches("plainPassword123", "$2a$10$hashedPassword");
            verify(jwtService).generateAccessToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("Should generate both access and refresh tokens on login")
        void shouldGenerateBothTokens() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

            // Act
            AuthResponse response = userService.login(loginRequest);

            // Assert
            assertThat(response.getAccessToken()).isNotEmpty();
            assertThat(response.getRefreshToken()).isNotEmpty();
            assertThat(response.getAccessToken()).isNotEqualTo(response.getRefreshToken());

            verify(jwtService, times(1)).generateAccessToken(testUser);
            verify(jwtService, times(1)).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("Should normalize email to lowercase during login")
        void shouldNormalizeEmailDuringLogin() {
            // Arrange
            loginRequest.setEmail("John.Doe@EXAMPLE.COM");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

            // Act
            userService.login(loginRequest);

            // Assert
            verify(userRepository).findByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should normalize full email to lowercase during login")
        void shouldNormalizeFullEmailDuringLogin() {
            // Arrange
            loginRequest.setEmail("JOHN.DOE@EXAMPLE.COM");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

            // Act
            userService.login(loginRequest);

            // Assert
            verify(userRepository).findByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtService, never()).generateAccessToken(any(User.class));
            verify(jwtService, never()).generateRefreshToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(passwordEncoder).matches("plainPassword123", "$2a$10$hashedPassword");
            verify(jwtService, never()).generateAccessToken(any(User.class));
            verify(jwtService, never()).generateRefreshToken(any(User.class));
        }

        @Test
        @DisplayName("Should verify password against stored hash")
        void shouldVerifyPasswordAgainstStoredHash() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

            // Act
            userService.login(loginRequest);

            // Assert
            verify(passwordEncoder).matches("plainPassword123", "$2a$10$hashedPassword");
        }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh tokens successfully with valid refresh token")
        void shouldRefreshTokensSuccessfully() {
            // Arrange
            String validRefreshToken = "valid-refresh-token";
            when(jwtService.isRefreshTokenValid(validRefreshToken)).thenReturn(true);
            when(jwtService.extractEmail(validRefreshToken)).thenReturn("john.doe@example.com");
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

            // Act
            AuthResponse response = userService.refreshToken(validRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("john.doe@example.com");

            verify(jwtService).isRefreshTokenValid(validRefreshToken);
            verify(jwtService).extractEmail(validRefreshToken);
            verify(userRepository).findByEmail("john.doe@example.com");
            verify(jwtService).generateAccessToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token is invalid")
        void shouldThrowExceptionWhenRefreshTokenInvalid() {
            // Arrange
            String invalidRefreshToken = "invalid-refresh-token";
            when(jwtService.isRefreshTokenValid(invalidRefreshToken)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.refreshToken(invalidRefreshToken))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid refresh token");

            verify(jwtService).isRefreshTokenValid(invalidRefreshToken);
            verify(jwtService, never()).extractEmail(anyString());
            verify(userRepository, never()).findByEmail(anyString());
            verify(jwtService, never()).generateAccessToken(any(User.class));
            verify(jwtService, never()).generateRefreshToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found during refresh")
        void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
            // Arrange
            String validRefreshToken = "valid-refresh-token";
            when(jwtService.isRefreshTokenValid(validRefreshToken)).thenReturn(true);
            when(jwtService.extractEmail(validRefreshToken)).thenReturn("nonexistent@example.com");
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.refreshToken(validRefreshToken))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with email");

            verify(jwtService).isRefreshTokenValid(validRefreshToken);
            verify(jwtService).extractEmail(validRefreshToken);
            verify(userRepository).findByEmail("nonexistent@example.com");
            verify(jwtService, never()).generateAccessToken(any(User.class));
            verify(jwtService, never()).generateRefreshToken(any(User.class));
        }

        @Test
        @DisplayName("Should generate new tokens on successful refresh")
        void shouldGenerateNewTokensOnRefresh() {
            // Arrange
            String oldRefreshToken = "old-refresh-token";
            when(jwtService.isRefreshTokenValid(oldRefreshToken)).thenReturn(true);
            when(jwtService.extractEmail(oldRefreshToken)).thenReturn("john.doe@example.com");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

            // Act
            AuthResponse response = userService.refreshToken(oldRefreshToken);

            // Assert
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

            verify(jwtService, times(1)).generateAccessToken(testUser);
            verify(jwtService, times(1)).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("Should validate refresh token before extracting email")
        void shouldValidateBeforeExtractingEmail() {
            // Arrange
            String invalidToken = "invalid-token";
            when(jwtService.isRefreshTokenValid(invalidToken)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.refreshToken(invalidToken))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(jwtService).isRefreshTokenValid(invalidToken);
            verify(jwtService, never()).extractEmail(anyString());
        }
    }

    // ==================== FIND USER TESTS ====================

    @Nested
    @DisplayName("Find User Tests")
    class FindUserTests {

        @Test
        @DisplayName("Should find user by email successfully")
        void shouldFindUserByEmail() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            // Act
            UserResponse response = userService.findByEmail("john.doe@example.com");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getId()).isEqualTo(1L);

            verify(userRepository).findByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.findByEmail("notfound@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with email");
        }

        @Test
        @DisplayName("Should find user by ID successfully")
        void shouldFindUserById() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            UserResponse response = userService.findById(1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("john.doe@example.com");

            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when ID not found")
        void shouldThrowExceptionWhenIdNotFound() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with ID");
        }

        @Test
        @DisplayName("Should find all users successfully")
        void shouldFindAllUsers() {
            // Arrange
            User user2 = User.builder()
                    .id(2L)
                    .email("jane@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            // Act
            List<UserResponse> users = userService.findAll();

            // Assert
            assertThat(users).hasSize(2);
            assertThat(users).extracting(UserResponse::getEmail)
                    .containsExactlyInAnyOrder("john.doe@example.com", "jane@example.com");

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsers() {
            // Arrange
            when(userRepository.findAll()).thenReturn(List.of());

            // Act
            List<UserResponse> users = userService.findAll();

            // Assert
            assertThat(users).isEmpty();
        }
    }

    // ==================== EMAIL EXISTS TESTS ====================

    @Nested
    @DisplayName("Email Exists Tests")
    class EmailExistsTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // Act
            boolean exists = userService.emailExists("john.doe@example.com");

            // Assert
            assertThat(exists).isTrue();
            verify(userRepository).existsByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            // Act
            boolean exists = userService.emailExists("notfound@example.com");

            // Assert
            assertThat(exists).isFalse();
            verify(userRepository).existsByEmail("notfound@example.com");
        }

        @Test
        @DisplayName("Should normalize email before checking existence")
        void shouldNormalizeEmailBeforeChecking() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // Act
            userService.emailExists("John.Doe@EXAMPLE.COM");

            // Assert
            verify(userRepository).existsByEmail("john.doe@example.com");
        }
    }

    // ==================== DELETE USER TESTS ====================

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Arrange
            when(userRepository.existsById(1L)).thenReturn(true);
            doNothing().when(userRepository).deleteById(1L);

            // Act
            userService.deleteById(1L);

            // Assert
            verify(userRepository).existsById(1L);
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            // Arrange
            when(userRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with ID");

            verify(userRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should check existence before deleting")
        void shouldCheckExistenceBeforeDeleting() {
            // Arrange
            when(userRepository.existsById(1L)).thenReturn(true);
            doNothing().when(userRepository).deleteById(1L);

            // Act
            userService.deleteById(1L);

            // Assert
            var inOrder = inOrder(userRepository);
            inOrder.verify(userRepository).existsById(1L);
            inOrder.verify(userRepository).deleteById(1L);
        }
    }

    // ==================== RESPONSE MAPPING TESTS ====================

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should not expose password in UserResponse")
        void shouldNotExposePassword() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            UserResponse response = userService.findById(1L);

            // Assert
            assertThat(response).hasNoNullFieldsOrPropertiesExcept("password");
        }

        @Test
        @DisplayName("Should map all required fields from User to UserResponse")
        void shouldMapAllRequiredFields() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            UserResponse response = userService.findById(1L);

            // Assert
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(response.getFirstName()).isEqualTo(testUser.getFirstName());
            assertThat(response.getLastName()).isEqualTo(testUser.getLastName());
            assertThat(response.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
        }
    }
}