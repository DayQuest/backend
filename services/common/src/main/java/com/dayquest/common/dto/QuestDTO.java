package com.dayquest.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Common Quest DTO for inter-service communication.
 */
public class QuestDTO {
    private UUID uuid;
    private UUID creatorUuid;
    private String title;
    private String description;
    private int likes;
    private int dislikes;
    private LocalDateTime createdAt;

    public QuestDTO() {}

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public UUID getCreatorUuid() { return creatorUuid; }
    public void setCreatorUuid(UUID creatorUuid) { this.creatorUuid = creatorUuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
