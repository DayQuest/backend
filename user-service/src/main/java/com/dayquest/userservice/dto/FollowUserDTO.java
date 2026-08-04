package com.dayquest.userservice.dto;

import java.util.UUID;

public class FollowUserDTO {

    private UUID uuid;
    private String username;
    private String profilePictureUrl;

    public FollowUserDTO() {}

    public FollowUserDTO(UUID uuid, String username, String profilePictureUrl) {
        this.uuid = uuid;
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
}