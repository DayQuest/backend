package com.dayquest.common.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * Common DTO for UUID transfer between services.
 */
public class UuidDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID uuid;

    public UuidDTO() {}

    public UuidDTO(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "UuidDTO{uuid=" + uuid + '}';
    }
}
