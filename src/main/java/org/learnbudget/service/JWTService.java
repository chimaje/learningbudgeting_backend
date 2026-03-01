package org.learnbudget.service;
import org.learnbudget.model.User;
import org.learnbudget.model.enums.TokenType;

public interface JWTService {

    /**
     * Generate access token for a user
     */
    String generateAccessToken(User user);

    /**
     * Generate refresh token for a user
     */
    String generateRefreshToken(User user);

    /**
     * Extract email from token
     */
    String extractEmail(String token);

    /**
     * Extract user ID from token
     */
    Long extractUserId(String token);

    /**
     * Validate access token
     */
    boolean isAccessTokenValid(String token, String email);

    /**
     * Validate refresh token
     */
    boolean isRefreshTokenValid(String token);

    /**
     * Check if token is expired
     */
    boolean isTokenExpired(String token);

    /**
     * Extract token type (ACCESS or REFRESH)
     */
    TokenType extractTokenType(String token);
}
