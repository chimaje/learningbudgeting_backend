package org.learnbudget.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.learnbudget.model.User;
import org.learnbudget.model.enums.TokenType;
import org.learnbudget.service.impl.JWTServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enhanced JWT Service Tests")
class JwtServiceTest {

    private JWTServiceImpl jwtService;
    private User testUser;

    private final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long ACCESS_EXPIRATION = 900000; // 15 minutes
    private final long REFRESH_EXPIRATION = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JWTServiceImpl();

        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        testUser = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== ACCESS TOKEN TESTS ====================

    @Nested
    @DisplayName("Access Token Tests")
    class AccessTokenTests {

        @Test
        @DisplayName("Should generate valid access token")
        void shouldGenerateAccessToken() {
            // Act
            String token = jwtService.generateAccessToken(testUser);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Should include ACCESS token type in claims")
        void shouldIncludeAccessTokenType() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            TokenType tokenType = jwtService.extractTokenType(token);

            // Assert
            assertThat(tokenType).isEqualTo(TokenType.ACCESS);
        }

        @Test
        @DisplayName("Should validate valid access token")
        void shouldValidateValidAccessToken() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            boolean isValid = jwtService.isAccessTokenValid(token, "john.doe@example.com");

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject access token with wrong email")
        void shouldRejectAccessTokenWithWrongEmail() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            boolean isValid = jwtService.isAccessTokenValid(token, "wrong@example.com");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject refresh token when validating as access token")
        void shouldRejectRefreshTokenAsAccessToken() {
            // Arrange
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Act
            boolean isValid = jwtService.isAccessTokenValid(refreshToken, "john.doe@example.com");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Access token should have correct expiration time")
        void accessTokenShouldHaveCorrectExpiration() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            // Assert
            long difference = expiration.getTime() - now.getTime();
            assertThat(difference).isCloseTo(ACCESS_EXPIRATION, org.assertj.core.data.Offset.offset(1000L));
        }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should generate valid refresh token")
        void shouldGenerateRefreshToken() {
            // Act
            String token = jwtService.generateRefreshToken(testUser);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Should include REFRESH token type in claims")
        void shouldIncludeRefreshTokenType() {
            // Arrange
            String token = jwtService.generateRefreshToken(testUser);

            // Act
            TokenType tokenType = jwtService.extractTokenType(token);

            // Assert
            assertThat(tokenType).isEqualTo(TokenType.REFRESH);
        }

        @Test
        @DisplayName("Should validate valid refresh token")
        void shouldValidateValidRefreshToken() {
            // Arrange
            String token = jwtService.generateRefreshToken(testUser);

            // Act
            boolean isValid = jwtService.isRefreshTokenValid(token);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject access token when validating as refresh token")
        void shouldRejectAccessTokenAsRefreshToken() {
            // Arrange
            String accessToken = jwtService.generateAccessToken(testUser);

            // Act
            boolean isValid = jwtService.isRefreshTokenValid(accessToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Refresh token should have longer expiration than access token")
        void refreshTokenShouldHaveLongerExpiration() {
            // Arrange
            String accessToken = jwtService.generateAccessToken(testUser);
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Act
            Claims accessClaims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

            Claims refreshClaims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            // Assert
            assertThat(refreshClaims.getExpiration()).isAfter(accessClaims.getExpiration());
        }

        @Test
        @DisplayName("Refresh token should have correct expiration time")
        void refreshTokenShouldHaveCorrectExpiration() {
            // Arrange
            String token = jwtService.generateRefreshToken(testUser);

            // Act
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            // Assert
            long difference = expiration.getTime() - now.getTime();
            assertThat(difference).isCloseTo(REFRESH_EXPIRATION, org.assertj.core.data.Offset.offset(1000L));
        }
    }

    // ==================== TOKEN EXTRACTION TESTS ====================

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract email from token")
        void shouldExtractEmail() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            String email = jwtService.extractEmail(token);

            // Assert
            assertThat(email).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should extract user ID from token")
        void shouldExtractUserId() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            Long userId = jwtService.extractUserId(token);

            // Assert
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should extract token type from access token")
        void shouldExtractAccessTokenType() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            TokenType tokenType = jwtService.extractTokenType(token);

            // Assert
            assertThat(tokenType).isEqualTo(TokenType.ACCESS);
        }

        @Test
        @DisplayName("Should extract token type from refresh token")
        void shouldExtractRefreshTokenType() {
            // Arrange
            String token = jwtService.generateRefreshToken(testUser);

            // Act
            TokenType tokenType = jwtService.extractTokenType(token);

            // Assert
            assertThat(tokenType).isEqualTo(TokenType.REFRESH);
        }

        @Test
        @DisplayName("Should include all user data in token claims")
        void shouldIncludeUserDataInClaims() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Assert
            assertThat(claims.getSubject()).isEqualTo("john.doe@example.com");
            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("firstName", String.class)).isEqualTo("John");
            assertThat(claims.get("lastName", String.class)).isEqualTo("Doe");
            assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        }
    }

    // ==================== TOKEN EXPIRATION TESTS ====================

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should detect non-expired token")
        void shouldDetectNonExpiredToken() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            boolean isExpired = jwtService.isTokenExpired(token);

            // Assert
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("Should reject invalid token format")
        void shouldRejectInvalidToken() {
            // Arrange
            String invalidToken = "invalid.token.format";

            // Act
            boolean isValid = jwtService.isAccessTokenValid(invalidToken, "john.doe@example.com");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // Act
            boolean isValid = jwtService.isAccessTokenValid("", "john.doe@example.com");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // Act
            boolean isValid = jwtService.isAccessTokenValid(null, "john.doe@example.com");

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    // ==================== TOKEN UNIQUENESS TESTS ====================

    @Nested
    @DisplayName("Token Uniqueness Tests")
    class TokenUniquenessTests {

        @Test
        @DisplayName("Should generate different access and refresh tokens")
        void shouldGenerateDifferentAccessAndRefreshTokens() {
            // Act
            String accessToken = jwtService.generateAccessToken(testUser);
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Assert
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Arrange
            User anotherUser = User.builder()
                    .id(2L)
                    .email("jane.smith@example.com")
                    .password("hashedPassword")
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();

            // Act
            String token1 = jwtService.generateAccessToken(testUser);
            String token2 = jwtService.generateAccessToken(anotherUser);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should generate different tokens at different times")
        void shouldGenerateDifferentTokensAtDifferentTimes() throws InterruptedException {
            // Act
            String token1 = jwtService.generateAccessToken(testUser);
            Thread.sleep(1000);
            String token2 = jwtService.generateAccessToken(testUser);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}