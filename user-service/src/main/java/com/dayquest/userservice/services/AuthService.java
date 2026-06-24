package com.dayquest.userservice.services;

import com.dayquest.common.dto.EmailTemplate;
import com.dayquest.common.exception.InvalidRequestException;
import com.dayquest.common.exception.UserAlreadyExistsException;
import com.dayquest.common.messaging.RabbitMQConstants;
import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {

    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    @Autowired
    private UserServiceJwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.base-url:https://apiv2.dayquest.de}")
    private String baseUrl;

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
            // Generate secure token for email verification link
            String verificationToken = UUID.randomUUID().toString();
            newUser.setVerificationCode(verificationToken);
            newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(24));
            newUser.setEnabled(false);
            newUser.setAuthorities(List.of("ROLE_USER"));
            userRepository.save(newUser);
            sendVerificationEmail(newUser);
    }
    private void sendVerificationEmail(User user) {
        String subject = "Verify Your DayQuest Account";
        String verificationToken = user.getVerificationCode();
        String verificationUrl = baseUrl + "/auth/verify?token=" + verificationToken;

        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center;\">"
                + "<h2 style=\"color: #333;\">Welcome to DayQuest!</h2>"
                + "<p style=\"font-size: 16px;\">Thank you for registering. Please verify your email address to activate your account.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); display: inline-block; margin: 20px 0;\">"
                + "<p style=\"margin-top: 20px; margin-bottom: 20px;\">"
                + "<a href=\"" + verificationUrl + "\" style=\"background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold;\">Verify Email Address</a>"
                + "</p>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #777;\">Or copy and paste this link into your browser:</p>"
                + "<p style=\"font-size: 12px; color: #007bff; word-break: break-all;\">" + verificationUrl + "</p>"
                + "<p style=\"font-size: 14px; color: #777; margin-top: 20px;\">This link is valid for 24 hours.</p>"
                + "<p style=\"font-size: 12px; color: #999; margin-top: 30px;\">If you didn't create an account, you can safely ignore this email.</p>"
                + "</div>"
                + "</body>"
                + "</html>";

        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setTo(user.getEmail());
        emailTemplate.setSubject(subject);
        emailTemplate.setBody(htmlMessage);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.EMAIL_ROUTING_KEY,
                emailTemplate
        );
        logger.info("Verification email sent to: {}", user.getEmail());
    }

    @Transactional
    public void verifyAccount(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRequestException("Verification token is required");
        }

        User user = userRepository.findByVerificationCode(token);
        if (user == null) {
            throw new InvalidRequestException("Invalid verification token");
        }

        // Check if token has expired
        if (user.getVerificationCodeExpiresAt() != null &&
            user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Verification link has expired. Please request a new one.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        logger.info("Account verified successfully for user: {}", user.getUsername());
    }

    /**
     * Resend verification email for users who haven't verified yet
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            // Don't reveal if email exists for security
            logger.info("Resend verification requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();
        if (user.isEnabled()) {
            throw new InvalidRequestException("Account is already verified");
        }

        // Generate new verification token
        String newToken = UUID.randomUUID().toString();
        user.setVerificationCode(newToken);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        sendVerificationEmail(user);
        logger.info("Resent verification email to: {}", email);
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

        String frontendResetUrl = baseUrl + "/auth/reset-password?token=" + token;

        String htmlMessage = String.format(
                "<html><body style=\"font-family: Arial, sans-serif;\">" +
                        "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center;\">" +
                        "<h2 style=\"color: #333;\">Password Reset Request</h2>" +
                        "<p style=\"font-size: 16px;\">You (or someone else) requested a password reset for your DayQuest account. " +
                        "If this was not you, please ignore this email.</p>" +
                        "<p style=\"font-size: 16px;\">To reset your password, please click the link below:</p>" +
                        "<p style=\"margin-top: 20px; margin-bottom: 20px;\"><a href=\"%s\" style=\"background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-size: 16px;\">Reset Your Password</a></p>" +
                        "<p style=\"font-size: 14px; color: #777;\">Or copy and paste this link:</p>" +
                        "<p style=\"font-size: 12px; color: #007bff; word-break: break-all;\">%s</p>" +
                        "<p style=\"font-size: 14px; color: #777; margin-top: 20px;\">This link is valid for 1 hour.</p>" +
                        "</div></body></html>",
                frontendResetUrl, frontendResetUrl);

        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setTo(user.getEmail());
        emailTemplate.setSubject(subject);
        emailTemplate.setBody(htmlMessage);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.EMAIL_ROUTING_KEY,
                emailTemplate
        );
        logger.info("Password reset email sent to: {}", user.getEmail());
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
}


