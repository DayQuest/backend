package com.dayquest.dayquestbackend.user;

import jakarta.annotation.Nullable;
import java.util.UUID;

public class LoginResponse {
  private UUID uuid;
  private String token;
  private String message;

  public LoginResponse(UUID uuid, String token, String message) {
    this.uuid = uuid;
    this.token = token;
    this.message = message;
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

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
