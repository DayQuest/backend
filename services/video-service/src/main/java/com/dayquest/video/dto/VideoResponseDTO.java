package com.dayquest.video.dto;

import com.dayquest.video.model.Video;
import com.dayquest.video.model.VideoStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class VideoResponseDTO {
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
    private VideoStatus status;
    private List<UUID> hashtagIds;
    private LocalDateTime createdAt;

    public VideoResponseDTO() {}

    public VideoResponseDTO(Video video) {
        this.uuid = video.getUuid();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.userUuid = video.getUserUuid();
        this.questUuid = video.getQuestUuid();
        this.upVotes = video.getUpVotes();
        this.downVotes = video.getDownVotes();
        this.views = video.getViews();
        this.comments = video.getComments();
        this.length = video.getLength();
        this.status = video.getStatus();
        this.hashtagIds = video.getHashtagIds();
        this.createdAt = video.getCreatedAt();
    }

    // Getters and Setters
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

    public VideoStatus getStatus() { return status; }
    public void setStatus(VideoStatus status) { this.status = status; }

    public List<UUID> getHashtagIds() { return hashtagIds; }
    public void setHashtagIds(List<UUID> hashtagIds) { this.hashtagIds = hashtagIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
