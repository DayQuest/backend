package com.dayquest.dayquestbackend.quest;

import java.util.UUID;

public class InteractionDTO {
    private UUID uuid;
    private UUID userUuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }
}