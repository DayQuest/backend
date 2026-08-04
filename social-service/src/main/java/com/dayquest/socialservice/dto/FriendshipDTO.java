package com.dayquest.socialservice.dto;

import com.dayquest.socialservice.model.FriendshipStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class FriendshipDTO {
    private UUID id;
    private UUID userUuid;
    private UUID friendUuid;
    private UUID otherUserUuid;
    private String otherUsername;
    private FriendshipStatus status;
    private LocalDateTime createdAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }

    public UUID getFriendUuid() {
        return friendUuid;
    }

    public void setFriendUuid(UUID friendUuid) {
        this.friendUuid = friendUuid;
    }

    public UUID getOtherUserUuid() {
        return otherUserUuid;
    }

    public void setOtherUserUuid(UUID otherUserUuid) {
        this.otherUserUuid = otherUserUuid;
    }

    public String getOtherUsername() {
        return otherUsername;
    }

    public void setOtherUsername(String otherUsername) {
        this.otherUsername = otherUsername;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

