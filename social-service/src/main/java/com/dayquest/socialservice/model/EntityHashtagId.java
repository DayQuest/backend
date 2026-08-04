package com.dayquest.socialservice.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class EntityHashtagId implements Serializable {
    private UUID entityId;
    private UUID hashtagId;

    public EntityHashtagId() {}

    public EntityHashtagId(UUID entityId, UUID hashtagId) {
        this.entityId = entityId;
        this.hashtagId = hashtagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityHashtagId that = (EntityHashtagId) o;
        return Objects.equals(entityId, that.entityId) && Objects.equals(hashtagId, that.hashtagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, hashtagId);
    }
}

