package org.learnbudget.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.learnbudget.model.User;
import org.learnbudget.model.enums.TokenType;
import org.learnbudget.service.JWTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JWTServiceImpl implements JWTService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    public String generateAccessToken(User user) {
        return generateToken(user, jwtExpiration, TokenType.ACCESS);
    }

    @Override
    public String generateRefreshToken(User user) {
        return generateToken(user, refreshExpiration, TokenType.REFRESH);
    }

    private String generateToken(User user, long expiration, TokenType tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("tokenType", tokenType.name());

        long currentTimeMillis = System.currentTimeMillis();//to ensure uniqueness

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(currentTimeMillis))  // Uses milliseconds
                .setExpiration(new Date(currentTimeMillis + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    @Override
    public TokenType extractTokenType(String token) {
        String type = extractClaim(token, claims -> claims.get("tokenType", String.class));
        return TokenType.valueOf(type);
    }

    @Override
    public boolean isAccessTokenValid(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            final TokenType tokenType = extractTokenType(token);
            return (tokenEmail.equals(email) &&
                    !isTokenExpired(token) &&
                    tokenType == TokenType.ACCESS);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isRefreshTokenValid(String token) {
        try {
            final TokenType tokenType = extractTokenType(token);
            return !isTokenExpired(token) && tokenType == TokenType.REFRESH;
        } catch (Exception e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}