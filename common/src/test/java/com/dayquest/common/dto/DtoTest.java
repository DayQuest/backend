package com.dayquest.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests")
class DtoTest {

    @Test
    @DisplayName("AuthTokenResponse should store all token information")
    void authTokenResponseShouldStoreTokenInfo() {
        AuthTokenResponse response = new AuthTokenResponse("access123", "refresh456", 900000L);

        assertEquals("access123", response.getAccessToken());
        assertEquals("refresh456", response.getRefreshToken());
        assertEquals(900000L, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("AuthTokenResponse default constructor should set Bearer type")
    void authTokenResponseDefaultShouldSetBearerType() {
        AuthTokenResponse response = new AuthTokenResponse();

        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("LoginResponse success factory should create successful response")
    void loginResponseSuccessFactory() {
        UUID userId = UUID.randomUUID();
        AuthTokenResponse tokens = new AuthTokenResponse("access", "refresh", 900000L);

        LoginResponse response = LoginResponse.success(userId, "john", tokens);

        assertTrue(response.isSuccess());
        assertEquals(userId, response.getUserId());
        assertEquals("john", response.getUsername());
        assertEquals(tokens, response.getTokens());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    @DisplayName("LoginResponse error factory should create error response")
    void loginResponseErrorFactory() {
        LoginResponse response = LoginResponse.error("Invalid credentials");

        assertFalse(response.isSuccess());
        assertEquals("Invalid credentials", response.getMessage());
        assertNull(response.getUserId());
        assertNull(response.getTokens());
    }

    @Test
    @DisplayName("RefreshTokenRequest should store refresh token")
    void refreshTokenRequestShouldStoreToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh123");

        assertEquals("refresh123", request.getRefreshToken());
    }

    @Test
    @DisplayName("ApiResponse success factory should create successful response")
    void apiResponseSuccessFactory() {
        ApiResponse<String> response = ApiResponse.success("Operation completed", "data");

        assertTrue(response.isSuccess());
        assertEquals("Operation completed", response.getMessage());
        assertEquals("data", response.getData());
    }

    @Test
    @DisplayName("ApiResponse error factory should create error response")
    void apiResponseErrorFactory() {
        ApiResponse<String> response = ApiResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("EmailTemplate should store email information")
    void emailTemplateShouldStoreEmailInfo() {
        EmailTemplate email = new EmailTemplate("test@example.com", "Subject", "Body");

        assertEquals("test@example.com", email.getTo());
        assertEquals("Subject", email.getSubject());
        assertEquals("Body", email.getBody());
    }

    @Test
    @DisplayName("EmailTemplate toString should contain all fields")
    void emailTemplateToStringShouldContainAllFields() {
        EmailTemplate email = new EmailTemplate("test@example.com", "Subject", "Body");

        String str = email.toString();

        assertTrue(str.contains("test@example.com"));
        assertTrue(str.contains("Subject"));
        assertTrue(str.contains("Body"));
    }

    @Test
    @DisplayName("PageInfo should store pagination information")
    void pageInfoShouldStorePaginationInfo() {
        PageInfo pageInfo = new PageInfo(0, 10, 100L, 10, true, false);

        assertEquals(0, pageInfo.getPage());
        assertEquals(10, pageInfo.getSize());
        assertEquals(100L, pageInfo.getTotalElements());
        assertEquals(10, pageInfo.getTotalPages());
        assertTrue(pageInfo.isFirst());
        assertFalse(pageInfo.isLast());
    }

    @Test
    @DisplayName("UuidDTO should store UUID")
    void uuidDtoShouldStoreUuid() {
        UUID uuid = UUID.randomUUID();
        UuidDTO dto = new UuidDTO(uuid);

        assertEquals(uuid, dto.getUuid());
    }
}

