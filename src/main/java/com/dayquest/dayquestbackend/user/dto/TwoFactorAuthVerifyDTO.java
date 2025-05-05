package com.dayquest.dayquestbackend.user.dto;

/**
 * DTO for verifying a 2FA code during login.
 */
public class TwoFactorAuthVerifyDTO {
    
    private String username;
    private String code;
    
    // Default constructor
    public TwoFactorAuthVerifyDTO() {
    }
    
    // Constructor with all fields
    public TwoFactorAuthVerifyDTO(String username, String code) {
        this.username = username;
        this.code = code;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}
