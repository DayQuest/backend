package com.dayquest.dayquestbackend.video.dto;

import com.dayquest.dayquestbackend.quest.Quest;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.annotation.Nullable;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.UUID;

public class VideoDTO {
    private String title;
    private String description;
    private Long upVotes;
    private Long downVotes;
    private UUID uuid;
    private String username;
    private String filePath;
    private String thumbnail;
    private boolean isFollowing;
    private LocalDateTime createdAt;
    private int views;

    @Transient
    @Nullable
    private boolean liked;

    @Transient
    @Nullable
    private boolean disliked;

    @JsonManagedReference
    private Quest quest;

    public VideoDTO(
            String title,
            String description,
            long upVotes,
            long downVotes,
            String username,
            String filePath,
            String thumbnail,
            Quest quest,
            UUID uuid,
            LocalDateTime createdAt,
            boolean liked) {
        this.title = title;
        this.createdAt = createdAt;
        this.description = description;
        this.upVotes = upVotes;
        this.downVotes = downVotes;
        this.username = username;
        this.filePath = filePath;
        this.thumbnail = thumbnail;
        this.uuid = uuid;
        this.quest = quest;
        this.liked = liked;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getUpVotes() { return upVotes; }
    public Long getDownVotes() { return downVotes; }
    public String getUsername() { return username; }
    public String getFilePath() { return filePath; }
    public String getThumbnail() { return thumbnail; }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUpVotes(Long upVotes) {
        this.upVotes = upVotes;
    }

    public void setDownVotes(Long downVotes) {
        this.downVotes = downVotes;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public boolean isDisliked() {
        return disliked;
    }

    public void setDisliked(boolean disliked) {
        this.disliked = disliked;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }
}
