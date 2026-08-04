package com.dayquest.common.dto;
import java.io.Serializable;
import java.util.UUID;
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID userId;
    private String username;
    private AuthTokenResponse tokens;
    private String message;
    private boolean success;
    public LoginResponse() {}
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public LoginResponse(UUID userId, String username, AuthTokenResponse tokens, String message) {
        this.userId = userId;
        this.username = username;
        this.tokens = tokens;
        this.message = message;
        this.success = true;
    }
    public static LoginResponse error(String message) {
        return new LoginResponse(false, message);
    }
    public static LoginResponse success(UUID userId, String username, AuthTokenResponse tokens) {
        return new LoginResponse(userId, username, tokens, "Login successful");
    }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public AuthTokenResponse getTokens() { return tokens; }
    public void setTokens(AuthTokenResponse tokens) { this.tokens = tokens; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
