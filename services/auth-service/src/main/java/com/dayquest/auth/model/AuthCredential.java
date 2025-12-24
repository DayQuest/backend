package com.dayquest.auth.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication credential entity storing password and authentication-related data.
 */
@Entity
@Table(name = "auth_credentials")
public class AuthCredential {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private String password;

    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiry;

    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;

    private boolean enabled;
    
    private boolean twoFactorEnabled;
    private String twoFactorSecret;

    private LocalDateTime lastLogin;
    private int failedLoginAttempts;
    private LocalDateTime lockoutUntil;

    // Constructors
    public AuthCredential() {}

    public AuthCredential(UUID userId, String password) {
        this.userId = userId;
        this.password = password;
        this.enabled = false;
        this.twoFactorEnabled = false;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public LocalDateTime getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) { 
        this.passwordResetTokenExpiry = passwordResetTokenExpiry; 
    }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public LocalDateTime getVerificationCodeExpiresAt() { return verificationCodeExpiresAt; }
    public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) { 
        this.verificationCodeExpiresAt = verificationCodeExpiresAt; 
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getTwoFactorSecret() { return twoFactorSecret; }
    public void setTwoFactorSecret(String twoFactorSecret) { this.twoFactorSecret = twoFactorSecret; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; }
}
