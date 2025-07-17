package com.dayquest.userservice.ids;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FollowId implements Serializable {

    private UUID userId;
    private UUID followedId;

    public FollowId() {}

    public FollowId(UUID userId, UUID followedId) {
        this.userId = userId;
        this.followedId = followedId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getFollowedId() {
        return followedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId)) return false;
        FollowId that = (FollowId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(followedId, that.followedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, followedId);
    }
}
