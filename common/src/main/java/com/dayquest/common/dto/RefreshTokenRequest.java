package com.dayquest.common.dto;
import java.io.Serializable;
public class RefreshTokenRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String refreshToken;
    public RefreshTokenRequest() {}
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
