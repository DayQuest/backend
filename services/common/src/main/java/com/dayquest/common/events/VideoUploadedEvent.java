package com.dayquest.common.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a video is uploaded.
 */
public class VideoUploadedEvent {
    private UUID videoId;
    private UUID userId;
    private UUID questId;
    private String title;
    private LocalDateTime uploadedAt;

    public VideoUploadedEvent() {}

    public VideoUploadedEvent(UUID videoId, UUID userId, UUID questId, String title, LocalDateTime uploadedAt) {
        this.videoId = videoId;
        this.userId = userId;
        this.questId = questId;
        this.title = title;
        this.uploadedAt = uploadedAt;
    }

    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getQuestId() { return questId; }
    public void setQuestId(UUID questId) { this.questId = questId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
