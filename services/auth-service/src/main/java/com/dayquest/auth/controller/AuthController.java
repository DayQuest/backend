package com.dayquest.auth.controller;

import com.dayquest.auth.dto.*;
import com.dayquest.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth Service", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDTO dto) {
        String result = authService.register(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify account with verification code")
    public ResponseEntity<String> verify(@RequestBody String verificationCode) {
        String userId = authService.verifyAccount(verificationCode.replace("\"", ""));
        return ResponseEntity.ok(userId);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code")
    public ResponseEntity<String> resendVerification(@RequestBody String email) {
        authService.resendVerificationCode(email.replace("\"", ""));
        return ResponseEntity.ok("Verification code resent");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        authService.forgotPassword(dto.getEmail());
        return ResponseEntity.ok("If an account with this email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        String result = authService.resetPassword(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable 2FA - returns QR code")
    public ResponseEntity<String> enable2FA(@RequestHeader("X-User-Id") String userId) {
        String qrCode = authService.enable2FA(UUID.fromString(userId));
        return ResponseEntity.ok(qrCode);
    }

    @PostMapping("/2fa/confirm")
    @Operation(summary = "Confirm 2FA setup with code")
    public ResponseEntity<String> confirm2FA(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody String code) {
        authService.confirm2FA(UUID.fromString(userId), code.replace("\"", ""));
        return ResponseEntity.ok("2FA enabled successfully");
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA")
    public ResponseEntity<String> disable2FA(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody String password) {
        authService.disable2FA(UUID.fromString(userId), password.replace("\"", ""));
        return ResponseEntity.ok("2FA disabled successfully");
    }
}
