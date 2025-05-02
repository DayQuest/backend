package com.dayquest.dayquestbackend.user.ids;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class VideoRatingId implements Serializable {

    private UUID userId;
    private UUID videoId;

    public VideoRatingId() {}

    public VideoRatingId(UUID user, UUID quest) {
        this.userId = user;
        this.videoId = quest;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoRatingId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(videoId, that.videoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, videoId);
    }
}
