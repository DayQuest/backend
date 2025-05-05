package com.dayquest.dayquestbackend.user.dto;

/**
 * DTO for responses to 2FA verification requests.
 */
public class TwoFactorAuthResponseDTO {
    
    private boolean success;
    private String token;
    private String uuid;
    private String message;
    
    // Default constructor
    public TwoFactorAuthResponseDTO() {
    }
    
    // Constructor for successful verification with token
    public TwoFactorAuthResponseDTO(boolean success, String token, String uuid) {
        this.success = success;
        this.token = token;
        this.uuid = uuid;
    }
    
    // Constructor for simple success/error message
    public TwoFactorAuthResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
