package com.dayquest.auth.service;

import com.dayquest.auth.client.UserServiceClient;
import com.dayquest.auth.dto.*;
import com.dayquest.auth.model.AuthCredential;
import com.dayquest.auth.model.BetaKey;
import com.dayquest.auth.repository.AuthCredentialRepository;
import com.dayquest.auth.repository.BetaKeyRepository;
import com.dayquest.common.exception.BadRequestException;
import com.dayquest.common.exception.ResourceNotFoundException;
import com.dayquest.common.exception.UnauthorizedException;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthCredentialRepository credentialRepository;
    private final BetaKeyRepository betaKeyRepository;
    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthCredentialRepository credentialRepository,
                       BetaKeyRepository betaKeyRepository,
                       UserServiceClient userServiceClient,
                       JwtService jwtService,
                       EmailService emailService,
                       TwoFactorAuthService twoFactorAuthService,
                       PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.betaKeyRepository = betaKeyRepository;
        this.userServiceClient = userServiceClient;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.twoFactorAuthService = twoFactorAuthService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String register(RegisterDTO dto) {
        // Validate beta key
        BetaKey betaKey = betaKeyRepository.findByKey(dto.getBetaKey())
                .orElseThrow(() -> new BadRequestException("Invalid beta key"));

        if (betaKey.isInUse()) {
            throw new BadRequestException("Beta key already in use");
        }

        // Create user in User Service
        var createUserRequest = new UserServiceClient.CreateUserRequest(
                dto.getUsername(), dto.getEmail(), dto.getPassword());
        
        var userResponse = userServiceClient.createUser(createUserRequest);
        if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
            throw new BadRequestException("Failed to create user");
        }

        UUID userId = userResponse.getBody().getUuid();

        // Create auth credentials
        AuthCredential credential = new AuthCredential(userId, passwordEncoder.encode(dto.getPassword()));
        credential.setVerificationCode(generateVerificationCode());
        credential.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
        credentialRepository.save(credential);

        // Mark beta key as used
        betaKey.setInUse(true);
        betaKey.setUsedByUsername(dto.getUsername());
        betaKeyRepository.save(betaKey);

        // Send verification email
        try {
            emailService.sendVerificationCode(dto.getEmail(), credential.getVerificationCode());
        } catch (MessagingException e) {
            logger.error("Failed to send verification email to {}: {}", dto.getEmail(), e.getMessage());
        }

        return "Successfully registered. Please check your email for verification code.";
    }

    @Transactional
    public LoginResponseDTO login(LoginDTO dto) {
        // Get user from User Service
        var userResponse = userServiceClient.getUserByUsername(dto.getUsername());
        if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        UserServiceDTO user = userResponse.getBody();
        AuthCredential credential = credentialRepository.findById(user.getUuid())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check if account is locked
        if (credential.getLockoutUntil() != null && credential.getLockoutUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("Account is temporarily locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(dto.getPassword(), credential.getPassword())) {
            handleFailedLogin(credential);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Check if account is enabled
        if (!credential.isEnabled()) {
            throw new UnauthorizedException("Account not verified. Please check your email.");
        }

        // Check if user is banned
        if (user.isBanned()) {
            throw new UnauthorizedException("Account is banned");
        }

        // Check 2FA
        if (credential.isTwoFactorEnabled()) {
            if (dto.getTwoFactorCode() == null || dto.getTwoFactorCode().isEmpty()) {
                return new LoginResponseDTO(null, user.getUuid(), user.getUsername(), true);
            }
            
            if (!twoFactorAuthService.verifyCode(credential.getTwoFactorSecret(), dto.getTwoFactorCode())) {
                throw new UnauthorizedException("Invalid 2FA code");
            }
        }

        // Reset failed login attempts
        credential.setFailedLoginAttempts(0);
        credential.setLockoutUntil(null);
        credential.setLastLogin(LocalDateTime.now());
        credentialRepository.save(credential);

        // Update last login in User Service
        userServiceClient.updateLastLogin(user.getUuid());

        // Generate token
        String token = jwtService.generateToken(user.getUuid(), user.getUsername());

        return new LoginResponseDTO(token, user.getUuid(), user.getUsername(), false);
    }

    @Transactional
    public String verifyAccount(String verificationCode) {
        AuthCredential credential = credentialRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new BadRequestException("Invalid verification code"));

        if (credential.getVerificationCodeExpiresAt() != null && 
            credential.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code has expired");
        }

        credential.setEnabled(true);
        credential.setVerificationCode(null);
        credential.setVerificationCodeExpiresAt(null);
        credentialRepository.save(credential);

        // Enable user in User Service
        userServiceClient.enableUser(credential.getUserId());

        return credential.getUserId().toString();
    }

    @Transactional
    public void resendVerificationCode(String email) {
        // Get user from User Service by email (would need to add this endpoint)
        // For now, we'll assume we have the email stored or can look up by it
        throw new UnsupportedOperationException("Implementation requires email lookup in User Service");
    }

    @Transactional
    public void forgotPassword(String email) {
        // This would need to communicate with User Service to get user by email
        // For now, log and return successfully regardless (security best practice)
        logger.info("Password reset requested for email: {}", email);
    }

    @Transactional
    public String resetPassword(ResetPasswordDTO dto) {
        AuthCredential credential = credentialRepository.findByPasswordResetToken(dto.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (credential.getPasswordResetTokenExpiry() == null ||
            credential.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            credential.setPasswordResetToken(null);
            credential.setPasswordResetTokenExpiry(null);
            credentialRepository.save(credential);
            throw new BadRequestException("Password reset token has expired");
        }

        credential.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        credential.setPasswordResetToken(null);
        credential.setPasswordResetTokenExpiry(null);
        credentialRepository.save(credential);

        return "Password has been reset successfully";
    }

    @Transactional
    public String enable2FA(UUID userId) {
        AuthCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String secret = twoFactorAuthService.generateSecret();
        credential.setTwoFactorSecret(secret);
        credentialRepository.save(credential);

        // Get username for QR code
        var userResponse = userServiceClient.getUserById(userId);
        String username = userResponse.getBody() != null ? userResponse.getBody().getUsername() : userId.toString();

        try {
            return twoFactorAuthService.generateQrCodeDataUri(secret, username);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Transactional
    public void confirm2FA(UUID userId, String code) {
        AuthCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (credential.getTwoFactorSecret() == null) {
            throw new BadRequestException("2FA not initialized");
        }

        if (!twoFactorAuthService.verifyCode(credential.getTwoFactorSecret(), code)) {
            throw new BadRequestException("Invalid 2FA code");
        }

        credential.setTwoFactorEnabled(true);
        credentialRepository.save(credential);
    }

    @Transactional
    public void disable2FA(UUID userId, String password) {
        AuthCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, credential.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        credential.setTwoFactorEnabled(false);
        credential.setTwoFactorSecret(null);
        credentialRepository.save(credential);
    }

    private void handleFailedLogin(AuthCredential credential) {
        int attempts = credential.getFailedLoginAttempts() + 1;
        credential.setFailedLoginAttempts(attempts);

        if (attempts >= 5) {
            credential.setLockoutUntil(LocalDateTime.now().plusMinutes(15));
        }

        credentialRepository.save(credential);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
