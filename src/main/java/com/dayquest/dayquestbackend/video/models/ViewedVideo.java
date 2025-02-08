package com.dayquest.dayquestbackend.video.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viewed_video")
public class ViewedVideo {

    @EmbeddedId
    private ViewedVideoId id;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    public ViewedVideo() {}

    public ViewedVideo(ViewedVideoId id) {
        this.id = id;
        this.viewedAt = LocalDateTime.now();
    }

    public ViewedVideoId getId() {
        return id;
    }

    public void setId(ViewedVideoId id) {
        this.id = id;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
