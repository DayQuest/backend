package com.dayquest.userservice.dto;

import java.util.UUID;

public class BadgeDTO {

    private UUID uuid;
    private String name;
    private String description;

    public BadgeDTO() {}

    public BadgeDTO(UUID uuid, String name, String description) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}