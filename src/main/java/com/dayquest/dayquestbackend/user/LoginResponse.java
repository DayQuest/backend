package com.dayquest.dayquestbackend.user;

import jakarta.annotation.Nullable;
import java.util.UUID;

public class LoginResponse {
  @Nullable
  private final UUID uuid;

  @Nullable
  private final String sessionToken;
  private final String message;

  public LoginResponse(@Nullable UUID uuid, @Nullable String sessionToken, String message) {
    this.uuid = uuid;
    this.sessionToken = sessionToken;
    this.message = message;
  }


  @Nullable
  public UUID getUuid() {
    return uuid;
  }

  @Nullable
  public String getSessionToken() {
    return sessionToken;
  }

  public String getMessage() {
    return message;
  }
}
