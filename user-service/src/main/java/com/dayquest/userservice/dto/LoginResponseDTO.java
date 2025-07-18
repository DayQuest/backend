package com.dayquest.userservice.dto;

import java.util.UUID;

public class LoginResponseDTO {
    private UUID uuid;
    private String token;
    private String message;

    public LoginResponseDTO(UUID uuid, String token, String message) {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
