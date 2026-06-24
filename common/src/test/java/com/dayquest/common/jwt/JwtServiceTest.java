package com.dayquest.common.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET_KEY = "dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate access token with user ID and roles")
    void shouldGenerateAccessToken() {
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        String token = jwtService.generateAccessToken(userId, roles);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertEquals(userId, jwtService.extractUserId(token));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    @DisplayName("Should generate refresh token with user ID")
    void shouldGenerateRefreshToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(userId);

        assertNotNull(token);
        assertEquals(userId, jwtService.extractUserId(token));
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Should extract roles from access token")
    void shouldExtractRoles() {
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
        String token = jwtService.generateAccessToken(userId, roles);

        List<String> extractedRoles = jwtService.extractRoles(token);

        assertNotNull(extractedRoles);
        assertEquals(2, extractedRoles.size());
        assertTrue(extractedRoles.contains("ROLE_USER"));
        assertTrue(extractedRoles.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should validate token with correct user ID")
    void shouldValidateTokenWithCorrectUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        boolean isValid = jwtService.isTokenValid(token, userId);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject token with wrong user ID")
    void shouldRejectTokenWithWrongUserId() {
        UUID userId = UUID.randomUUID();
        UUID wrongUserId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        boolean isValid = jwtService.isTokenValid(token, wrongUserId);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L); // Already expired
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        assertTrue(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should correctly identify access token type")
    void shouldIdentifyAccessTokenType() {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, List.of("ROLE_USER"));
        String refreshToken = jwtService.generateRefreshToken(userId);

        assertTrue(jwtService.isAccessToken(accessToken));
        assertFalse(jwtService.isAccessToken(refreshToken));
    }

    @Test
    @DisplayName("Should correctly identify refresh token type")
    void shouldIdentifyRefreshTokenType() {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, List.of("ROLE_USER"));
        String refreshToken = jwtService.generateRefreshToken(userId);

        assertTrue(jwtService.isRefreshToken(refreshToken));
        assertFalse(jwtService.isRefreshToken(accessToken));
    }

    @Test
    @DisplayName("Should return correct expiration times")
    void shouldReturnCorrectExpirationTimes() {
        assertEquals(ACCESS_TOKEN_EXPIRATION, jwtService.getAccessTokenExpiration());
        assertEquals(REFRESH_TOKEN_EXPIRATION, jwtService.getRefreshTokenExpiration());
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void shouldThrowExceptionForInvalidToken() {
        assertThrows(RuntimeException.class, () -> {
            jwtService.extractUserId("invalid-token");
        });
    }

    @Test
    @DisplayName("Should extract token type correctly")
    void shouldExtractTokenType() {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, List.of("ROLE_USER"));
        String refreshToken = jwtService.generateRefreshToken(userId);

        assertEquals("access", jwtService.extractTokenType(accessToken));
        assertEquals("refresh", jwtService.extractTokenType(refreshToken));
    }
}

