package com.dayquest.userservice.controllers;

import com.dayquest.userservice.dto.*;
import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.exceptions.InvalidRequestException;
import com.dayquest.userservice.exceptions.UserAlreadyExistsException;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import com.dayquest.userservice.services.AuthService;
import com.dayquest.userservice.services.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AuthService authService, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try{
            authService.register(registerDTO.getUsername(), registerDTO.getEmail(), registerDTO.getPassword());
            return ResponseEntity.ok("Registered Successfully");
        } catch (InvalidRequestException | UserAlreadyExistsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        User user = userRepository.findByUsername(loginDTO.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new LoginResponseDTO(null, null, "User not found"));
        }
        if (user.getPunishment() == Punishments.BANNED || user.getPunishment() == Punishments.TEMP_BANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new LoginResponseDTO(null, null, "User has been banned"));
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO(null, null, "Invalid password"));
        }

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO(null, null, "User not verified"));
        }

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new LoginResponseDTO(user.getUuid(), token, "Login successful"));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String code) {
        try{
            authService.verifyAccount(code);
            return ResponseEntity.ok().build();
        }catch (InvalidRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/token/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token, @RequestBody UUID uuid) {
        boolean isValid = authService.tokenUuidValid(token.substring(7), uuid);
        return ResponseEntity.ok(isValid);
    }


    @PostMapping("/forgot-password")
    @Async
    public CompletableFuture<ResponseEntity<String>> forgotPassword(@RequestBody @Valid ForgotPasswordDTO forgotPasswordRequestDTO) {
        return authService.handleForgotPasswordRequest(forgotPasswordRequestDTO.getEmail())
                .thenApply(success -> ResponseEntity.ok("If an account with this email exists, a password reset link has been sent."))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + ex.getMessage()));
    }

    @PostMapping("/reset-password")
    @Async
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
                    const response = await fetch('/api/users/reset-password', {
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
