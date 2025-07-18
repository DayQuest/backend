package com.dayquest.userservice.services;

import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.exceptions.InvalidRequestException;
import com.dayquest.userservice.exceptions.UserAlreadyExistsException;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthService {

    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(String username, String email, String password) {
            if(!password.matches("^[\\x21-\\x7E]+$")){
                throw new InvalidRequestException("Invalid password");
            }
            if(userRepository.findByEmailIgnoreCase(email).isPresent() || userRepository.findByUsername(username.toLowerCase()) != null){
                throw new UserAlreadyExistsException("User with this email or username already exists");
            }

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setVerificationCode(generateVerificationCode());
            newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            newUser.setEnabled(false);
            newUser.setAuthorities(List.of("ROLE_USER"));

            //TODO: Send verification email
            //TODO: Set Daily Quest

    }
    @Async
    public void verifyAccount(String verificationCode) {
        System.out.println(verificationCode);
        User user = userRepository.findByVerificationCode(verificationCode);
        if (user != null) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
        } else {
            throw new InvalidRequestException("Invalid verification code");
        }
    }

    @Async
    @Transactional
    public CompletableFuture<Boolean> handleForgotPasswordRequest(String email) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (!user.isEnabled()) {
                    logger.warn("Password reset requested for unverified account: {}", email);
                }

                String token = UUID.randomUUID().toString();
                user.setPasswordResetToken(token);
                user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
                userRepository.save(user);

                try {
                    sendPasswordResetEmailInternal(user, token);

                } catch (Exception e) {
                    logger.error("Failed to send password reset email to {}: {}", email, e.getMessage());
                }
            } else {
                logger.info("Password reset requested for non-existent email: {}", email);
            }
            return CompletableFuture.completedFuture(true);
    }

    private void sendPasswordResetEmailInternal(User user, String token){
        String subject = "DayQuest Password Reset Request";

        String frontendResetUrl = "https://apiv2.dayquest.de/api/users/reset-password?token=" + token;

        String htmlMessage = String.format(
                "<html><body style=\"font-family: Arial, sans-serif;\">" +
                        "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center;\">" +
                        "<h2 style=\"color: #333;\">Password Reset Request</h2>" +
                        "<p style=\"font-size: 16px;\">You (or someone else) requested a password reset for your DayQuest account. " +
                        "If this was not you, please ignore this email.</p>" +
                        "<p style=\"font-size: 16px;\">To reset your password, please click the link below.:</p>" +
                        "<p style=\"margin-top: 20px; margin-bottom: 20px;\"><a href=\"%s\" style=\"background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-size: 16px;\">Reset Your Password</a></p>" +
                        "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); display: inline-block; margin-top:10px;\">" +
                        "</div>" +
                        "<p style=\"font-size: 14px; color: #777; margin-top: 20px;\">This link and token are valid for 1 hour.</p>" +
                        "</div></body></html>",
                frontendResetUrl);

        //TODO: Implement email sending logic
    }

    @Async
    @Transactional
    public CompletableFuture<String> handleResetPassword(String token, String newPassword) {
            if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
                throw new IllegalArgumentException("Token and new password must not be empty.");
            }
            Optional<User> userOptional = userRepository.findByPasswordResetToken(token);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPasswordResetTokenExpiry() != null && user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now())) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setPasswordResetToken(null);
                    user.setPasswordResetTokenExpiry(null);
                    userRepository.save(user);
                    return CompletableFuture.completedFuture("Password has been reset successfully.");
                } else {
                    user.setPasswordResetToken(null);
                    user.setPasswordResetTokenExpiry(null);
                    userRepository.save(user);
                    throw new IllegalArgumentException("Password reset token is invalid or has expired.");
                }
            } else {
                throw new IllegalArgumentException("Password reset token is invalid or has expired.");
            }

    }

    public boolean tokenUuidValid(String token, UUID uuid) {
        Optional<User> userOptional = userRepository.findById(uuid);
        if (userOptional.isEmpty() || userOptional.get().getPunishment() == Punishments.TEMP_BANNED || userOptional.get().getPunishment() == Punishments.BANNED) {
            return false;
        }
        User user = userOptional.get();
        return jwtService.isTokenValid(token, user);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}


