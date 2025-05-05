package com.dayquest.dayquestbackend.user.dto;

/**
 * DTO for transferring 2FA information between client and server.
 */
public class TwoFactorAuthDTO {
    
    private String secretKey;
    private String qrCodeUrl;
    private String otpAuthUrl;
    private boolean enabled;
    private String code;
    
    // Default constructor
    public TwoFactorAuthDTO() {
    }
    
    // Constructor for setup information
    public TwoFactorAuthDTO(String secretKey, String qrCodeUrl, String otpAuthUrl) {
        this.secretKey = secretKey;
        this.qrCodeUrl = qrCodeUrl;
        this.otpAuthUrl = otpAuthUrl;
    }
    
    // Constructor for status information
    public TwoFactorAuthDTO(boolean enabled) {
        this.enabled = enabled;
    }
    
    // Getters and Setters
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }
    
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
    
    public String getOtpAuthUrl() {
        return otpAuthUrl;
    }
    
    public void setOtpAuthUrl(String otpAuthUrl) {
        this.otpAuthUrl = otpAuthUrl;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}
