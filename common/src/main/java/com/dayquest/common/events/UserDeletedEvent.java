package com.dayquest.common.events;

import java.io.Serializable;
import java.util.UUID;

/**
 * Event fired when a user is deleted.
 * All services that hold user-related data should listen for this event
 * and delete the associated data.
 */
public class UserDeletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String username;

    public UserDeletedEvent() {
    }

    public UserDeletedEvent(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "UserDeletedEvent{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }
}

