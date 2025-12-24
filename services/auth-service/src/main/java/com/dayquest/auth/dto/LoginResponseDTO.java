package com.dayquest.auth.dto;

import java.util.UUID;

public class LoginResponseDTO {

    private String token;
    private UUID userId;
    private String username;
    private boolean twoFactorRequired;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, UUID userId, String username, boolean twoFactorRequired) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.twoFactorRequired = twoFactorRequired;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isTwoFactorRequired() { return twoFactorRequired; }
    public void setTwoFactorRequired(boolean twoFactorRequired) { this.twoFactorRequired = twoFactorRequired; }
}
