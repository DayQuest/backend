package com.dayquest.userservice.controllers;

import com.dayquest.common.dto.AuthTokenResponse;
import com.dayquest.common.dto.LoginResponse;
import com.dayquest.common.dto.RefreshTokenRequest;
import com.dayquest.common.exception.InvalidRequestException;
import com.dayquest.common.exception.UserAlreadyExistsException;
import com.dayquest.userservice.dto.*;
import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import com.dayquest.userservice.services.AuthService;
import com.dayquest.userservice.services.UserServiceJwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, token management, and password reset")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserServiceJwtService jwtService;

    public AuthController(AuthService authService, UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder, UserServiceJwtService jwtService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account. A verification email is sent to the provided address. " +
                    "The account must be verified before login is possible.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful — verification email sent",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already in use",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try{
            authService.register(registerDTO.getUsername(), registerDTO.getEmail(), registerDTO.getPassword());
            return ResponseEntity.ok("Registered Successfully");
        } catch (InvalidRequestException | UserAlreadyExistsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login and obtain JWT tokens",
            description = "Authenticates the user with username and password. Returns an access token (15 min TTL) " +
                    "and a refresh token (7 day TTL). Banned users cannot log in.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — returns access and refresh tokens",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid password or unverified account",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "Account is banned",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
        User user = userRepository.findByUsername(loginDTO.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(LoginResponse.error("User not found"));
        }
        if (user.getPunishment() == Punishments.BANNED || user.getPunishment() == Punishments.TEMP_BANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.error("User has been banned"));
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.error("Invalid password"));
        }

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.error("User not verified"));
        }

        AuthTokenResponse tokens = jwtService.generateTokenPair(user);
        return ResponseEntity.ok(LoginResponse.success(user.getUuid(), user.getUsername(), tokens));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access token and refresh token pair. " +
                    "Use this when the access token has expired.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired")
    })
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            AuthTokenResponse tokens = jwtService.refreshTokens(request.getRefreshToken());
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.error("Invalid or expired refresh token"));
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout (invalidate session)",
            description = "Logs out the currently authenticated user. The client should discard stored tokens.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok("Logged out successfully");
    }

    /**
     * Verify account via GET request with token parameter (clicked from email link)
     */
    @GetMapping("/verify")
    @Operation(
            summary = "Verify email address (link click)",
            description = "Verifies the user's email address using the token from the verification email. " +
                    "This endpoint is intended for email link clicks and returns an HTML page.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account verified — HTML confirmation page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token — HTML error page returned")
    })
    public ResponseEntity<String> verifyViaLink(
            @Parameter(description = "Verification token from email link", required = true)
            @RequestParam String token) {
        try {
            authService.verifyAccount(token);
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DayQuest - Account Verified</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            min-height: 100vh;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 10px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 400px;
                        }
                        h1 { color: #28a745; margin-bottom: 20px; }
                        p { color: #666; margin-bottom: 20px; }
                        .icon { font-size: 60px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">✓</div>
                        <h1>Email Verified!</h1>
                        <p>Your account has been successfully verified. You can now log in to DayQuest.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        } catch (InvalidRequestException e) {
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DayQuest - Verification Failed</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            min-height: 100vh;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 10px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 400px;
                        }
                        h1 { color: #dc3545; margin-bottom: 20px; }
                        p { color: #666; margin-bottom: 20px; }
                        .icon { font-size: 60px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">✗</div>
                        <h1>Verification Failed</h1>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "text/html")
                    .body(html);
        }
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify email address (API)",
            description = "Verifies the user's email address using the token from the verification email. " +
                    "This endpoint is for programmatic use and returns a plain text response.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    })
    public ResponseEntity<?> verify(
            @Parameter(description = "Verification token from email", required = true)
            @RequestParam String token) {
        try {
            authService.verifyAccount(token);
            return ResponseEntity.ok("Account verified successfully");
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    @Operation(
            summary = "Resend verification email",
            description = "Resends the email verification link to the given address if an unverified account exists. " +
                    "Always returns 200 to prevent email enumeration.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "If account exists, a new verification email was sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<String> resendVerification(@RequestBody @Valid EmailDTO request) {
        try {
            authService.resendVerificationEmail(request.getEmail());
            return ResponseEntity.ok("If an unverified account with this email exists, a new verification email has been sent.");
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/token/validate")
    @Operation(
            summary = "Validate a JWT token",
            description = "Validates that a given JWT token is authentic and belongs to the specified user UUID. " +
                    "Used internally by other services to verify token authenticity.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns true if valid, false otherwise",
                    content = @Content(schema = @Schema(implementation = Boolean.class)))
    })
    public ResponseEntity<Boolean> validateToken(
            @Parameter(description = "Bearer token to validate", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "User UUID that the token should belong to", required = true)
            @RequestBody UUID uuid) {
        boolean isValid = authService.tokenUuidValid(token.substring(7), uuid);
        return ResponseEntity.ok(isValid);
    }


    @PostMapping("/forgot-password")
    @Async
    @Operation(
            summary = "Request password reset email",
            description = "Sends a password reset link to the provided email address if an account exists. " +
                    "Always returns 200 to prevent email enumeration.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "If account exists, a password reset link was sent")
    })
    public CompletableFuture<ResponseEntity<String>> forgotPassword(@RequestBody @Valid EmailDTO forgotPasswordRequestDTO) {
        return authService.handleForgotPasswordRequest(forgotPasswordRequestDTO.getEmail())
                .thenApply(success -> ResponseEntity.ok("If an account with this email exists, a password reset link has been sent."))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + ex.getMessage()));
    }

    @PostMapping("/reset-password")
    @Async
    @Operation(
            summary = "Reset password using token",
            description = "Resets the user's password using the token from the password reset email.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token, or weak password"),
            @ApiResponse(responseCode = "500", description = "Internal error during reset")
    })
    public CompletableFuture<ResponseEntity<String>> resetPassword(@RequestBody ResetPasswordRequestDTO resetPasswordDTO) {
        return authService.handleResetPassword(resetPasswordDTO.getToken(), resetPasswordDTO.getNewPassword())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return ResponseEntity.badRequest().body(ex.getCause().getMessage());
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred while resetting the password.");
                });
    }

    @GetMapping("/reset-password")
    @Operation(
            summary = "Password reset form (HTML)",
            description = "Returns an HTML page with a password reset form. Intended to be opened via browser link from the reset email.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTML form returned")
    })
    public ResponseEntity<String> resetPasswordPage(@RequestParam(required = false) String token) {
        String html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>DayQuest - Reset Password</title>
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                display: flex;
                justify-content: center;
                align-items: center;
                padding: 20px;
            }
            
            .container {
                background: white;
                border-radius: 15px;
                box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                padding: 40px;
                width: 100%;
                max-width: 450px;
                text-align: center;
            }
            
            .logo {
                font-size: 2.5em;
                font-weight: bold;
                color: #667eea;
                margin-bottom: 10px;
            }
            
            .subtitle {
                color: #666;
                margin-bottom: 30px;
                font-size: 16px;
            }
            
            .form-group {
                margin-bottom: 20px;
                text-align: left;
            }
            
            label {
                display: block;
                margin-bottom: 8px;
                color: #333;
                font-weight: 500;
            }
            
            input[type="password"] {
                width: 100%;
                padding: 15px;
                border: 2px solid #e1e5e9;
                border-radius: 8px;
                font-size: 16px;
                transition: border-color 0.3s ease;
            }
            
            input[type="password"]:focus {
                outline: none;
                border-color: #667eea;
            }
            
            .btn {
                width: 100%;
                padding: 15px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                font-weight: 600;
                cursor: pointer;
                transition: transform 0.2s ease;
                margin-top: 10px;
            }
            
            .btn:hover {
                transform: translateY(-2px);
            }
            
            .btn:disabled {
                opacity: 0.6;
                cursor: not-allowed;
                transform: none;
            }
            
            .message {
                padding: 15px;
                border-radius: 8px;
                margin-bottom: 20px;
                font-weight: 500;
            }
            
            .error {
                background-color: #fee;
                border: 1px solid #fcc;
                color: #c33;
            }
            
            .success {
                background-color: #efe;
                border: 1px solid #cfc;
                color: #363;
            }
            
            .loading {
                display: none;
                margin-top: 10px;
            }
            
            .spinner {
                border: 3px solid #f3f3f3;
                border-top: 3px solid #667eea;
                border-radius: 50%;
                width: 30px;
                height: 30px;
                animation: spin 1s linear infinite;
                margin: 0 auto;
            }
            
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
            
            .password-requirements {
                font-size: 14px;
                color: #666;
                text-align: left;
                margin-top: 5px;
            }
            
            .password-requirements ul {
                margin-left: 20px;
                margin-top: 5px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="logo">DayQuest</div>
            <div class="subtitle">Reset Your Password</div>
            
            <div id="message"></div>
            
            <form id="resetForm">
                <div class="form-group">
                    <label for="newPassword">New Password:</label>
                    <input type="password" id="newPassword" name="newPassword" required minlength="6">
                    <div class="password-requirements">
                        <p>Password requirements:</p>
                        <ul>
                            <li>At least 6 characters long</li>
                            <li>Mix of letters and numbers recommended</li>
                        </ul>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="confirmPassword">Confirm New Password:</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                </div>
                
                <button type="submit" class="btn" id="submitBtn">Reset Password</button>
                
                <div class="loading" id="loading">
                    <div class="spinner"></div>
                    <p>Resetting password...</p>
                </div>
            </form>
        </div>
        
        <script>
            const urlParams = new URLSearchParams(window.location.search);
            const token = urlParams.get('token');
            const messageDiv = document.getElementById('message');
            const form = document.getElementById('resetForm');
            const submitBtn = document.getElementById('submitBtn');
            const loading = document.getElementById('loading');
            
            if (!token) {
                showMessage('Invalid or missing reset token. Please request a new password reset.', 'error');
                submitBtn.disabled = true;
            }
            
            form.addEventListener('submit', async function(e) {
                e.preventDefault();
                
                const newPassword = document.getElementById('newPassword').value;
                const confirmPassword = document.getElementById('confirmPassword').value;
                
                if (newPassword !== confirmPassword) {
                    showMessage('Passwords do not match. Please try again.', 'error');
                    return;
                }
                
                if (newPassword.length < 6) {
                    showMessage('Password must be at least 6 characters long.', 'error');
                    return;
                }
                
                submitBtn.disabled = true;
                loading.style.display = 'block';
                messageDiv.innerHTML = '';
                
                try {
                    const response = await fetch('/auth/reset-password', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            token: token,
                            newPassword: newPassword
                        })
                    });
                    
                    const result = await response.text();
                    
                    if (response.ok) {
                        showMessage('Password reset successfully! You can now log in with your new password.', 'success');
                        form.style.display = 'none';
                    } else {
                        showMessage(result || 'Failed to reset password. Please try again.', 'error');
                        submitBtn.disabled = false;
                    }
                } catch (error) {
                    showMessage('Network error. Please check your connection and try again.', 'error');
                    submitBtn.disabled = false;
                } finally {
                    loading.style.display = 'none';
                }
            });
            
            function showMessage(text, type) {
                messageDiv.innerHTML = `<div class="message ${type}">${text}</div>`;
            }
        </script>
    </body>
    </html>
    """;

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

}
