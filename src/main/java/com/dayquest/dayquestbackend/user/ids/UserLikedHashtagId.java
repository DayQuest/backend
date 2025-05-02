package com.dayquest.dayquestbackend.user.ids;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class UserLikedHashtagId implements Serializable {

    private UUID userId;
    private UUID hashtagId;

    public UserLikedHashtagId() {}

    public UserLikedHashtagId(UUID user, UUID quest) {
        this.userId = user;
        this.hashtagId = quest;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getHashtagId() {
        return hashtagId;
    }

    public void setHashtagId(UUID hashtagId) {
        this.hashtagId = hashtagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserLikedHashtagId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(hashtagId, that.hashtagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, hashtagId);
    }
}

