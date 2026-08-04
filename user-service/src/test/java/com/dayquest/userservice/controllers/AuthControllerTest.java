package com.dayquest.userservice.controllers;

import com.dayquest.common.dto.AuthTokenResponse;
import com.dayquest.common.dto.LoginResponse;
import com.dayquest.common.dto.RefreshTokenRequest;
import com.dayquest.userservice.dto.LoginDTO;
import com.dayquest.userservice.dto.RegisterDTO;
import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import com.dayquest.userservice.services.AuthService;
import com.dayquest.userservice.services.UserServiceJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserServiceJwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private AuthTokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUuid(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setEnabled(true);
        testUser.setPunishment(Punishments.NONE);
        testUser.setAuthorities(List.of("ROLE_USER"));

        tokenResponse = new AuthTokenResponse("access_token", "refresh_token", 900000L);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtService.generateTokenPair(any(User.class))).thenReturn(tokenResponse);

        ResponseEntity<LoginResponse> response = authController.login(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("access_token", response.getBody().getTokens().getAccessToken());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("unknown");
        loginDTO.setPassword("password");

        when(userRepository.findByUsername("unknown")).thenReturn(null);

        ResponseEntity<LoginResponse> response = authController.login(loginDTO);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return 403 when user is banned")
    void shouldReturn403WhenUserIsBanned() {
        testUser.setPunishment(Punishments.BANNED);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password");

        when(userRepository.findByUsername("testuser")).thenReturn(testUser);

        ResponseEntity<LoginResponse> response = authController.login(loginDTO);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User has been banned", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return 401 when password is invalid")
    void shouldReturn401WhenPasswordInvalid() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        ResponseEntity<LoginResponse> response = authController.login(loginDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid password", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return 401 when user is not verified")
    void shouldReturn401WhenUserNotVerified() {
        testUser.setEnabled(false);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

        ResponseEntity<LoginResponse> response = authController.login(loginDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not verified", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should refresh tokens successfully")
    void shouldRefreshTokensSuccessfully() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");
        AuthTokenResponse newTokens = new AuthTokenResponse("new_access", "new_refresh", 900000L);

        when(jwtService.refreshTokens("valid_refresh_token")).thenReturn(newTokens);

        ResponseEntity<?> response = authController.refreshToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        AuthTokenResponse body = (AuthTokenResponse) response.getBody();
        assertEquals("new_access", body.getAccessToken());
        assertEquals("new_refresh", body.getRefreshToken());
    }

    @Test
    @DisplayName("Should return 401 when refresh token is invalid")
    void shouldReturn401WhenRefreshTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid_token");

        when(jwtService.refreshTokens("invalid_token")).thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<?> response = authController.refreshToken(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Should register successfully")
    void shouldRegisterSuccessfully() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setEmail("new@example.com");
        registerDTO.setPassword("password123");

        ResponseEntity<String> response = authController.register(registerDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Registered Successfully", response.getBody());
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        ResponseEntity<String> response = authController.logout("Bearer some_token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully", response.getBody());
    }
}

