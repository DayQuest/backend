package com.dayquest.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Common Video DTO for inter-service communication.
 */
public class VideoDTO {
    private UUID uuid;
    private String title;
    private String description;
    private UUID userUuid;
    private UUID questUuid;
    private int upVotes;
    private int downVotes;
    private int views;
    private int comments;
    private float length;
    private LocalDateTime createdAt;

    public VideoDTO() {}

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }
    public UUID getQuestUuid() { return questUuid; }
    public void setQuestUuid(UUID questUuid) { this.questUuid = questUuid; }
    public int getUpVotes() { return upVotes; }
    public void setUpVotes(int upVotes) { this.upVotes = upVotes; }
    public int getDownVotes() { return downVotes; }
    public void setDownVotes(int downVotes) { this.downVotes = downVotes; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }
    public float getLength() { return length; }
    public void setLength(float length) { this.length = length; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
