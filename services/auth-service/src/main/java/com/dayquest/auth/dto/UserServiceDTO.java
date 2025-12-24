package com.dayquest.auth.dto;

import java.util.UUID;

/**
 * DTO for user data received from User Service.
 */
public class UserServiceDTO {
    private UUID uuid;
    private String username;
    private String email;
    private int followers;
    private boolean enabled;
    private boolean banned;

    public UserServiceDTO() {}

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

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
