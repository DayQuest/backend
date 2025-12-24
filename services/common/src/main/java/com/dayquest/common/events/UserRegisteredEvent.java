package com.dayquest.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a new user is registered.
 */
public class UserRegisteredEvent {
    private UUID userId;
    private String username;
    private String email;
    private LocalDateTime registeredAt;

    public UserRegisteredEvent() {}

    public UserRegisteredEvent(UUID userId, String username, String email, LocalDateTime registeredAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
