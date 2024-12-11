package com.dayquest.dayquestbackend.video;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ViewedVideoId implements Serializable {
    private UUID userId;
    private UUID videoId;

    public ViewedVideoId() {}

    public ViewedVideoId(UUID userId, UUID videoId) {
        this.userId = userId;
        this.videoId = videoId;
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
        if (o == null || getClass() != o.getClass()) return false;
        ViewedVideoId that = (ViewedVideoId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(videoId, that.videoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, videoId);
    }
}
