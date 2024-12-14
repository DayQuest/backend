package com.dayquest.dayquestbackend.user;

import java.util.UUID;

public class UpdateUserDTO {
    private UUID uuid;
    private String username;

    // Getter und Setter
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
