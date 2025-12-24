package com.dayquest.common.dto;

import java.util.UUID;

/**
 * Common User DTO for inter-service communication.
 * This DTO is used when services need to exchange user information.
 */
public class UserDTO {
    private UUID uuid;
    private String username;
    private String email;
    private int followers;
    private boolean enabled;

    public UserDTO() {}

    public UserDTO(UUID uuid, String username, String email, int followers, boolean enabled) {
        this.uuid = uuid;
        this.username = username;
        this.email = email;
        this.followers = followers;
        this.enabled = enabled;
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
