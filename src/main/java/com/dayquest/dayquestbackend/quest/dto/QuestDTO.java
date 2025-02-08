package com.dayquest.dayquestbackend.quest.dto;

import com.dayquest.dayquestbackend.quest.Quest;

import java.time.LocalDateTime;
import java.util.UUID;

public class QuestDTO {
    private UUID uuid;
    private UUID creatorUuid;
    private String title;
    private String description;
    private int likes;
    private int dislikes;
    private LocalDateTime createdAt;
    private boolean isLiked;
    private boolean isDisliked;

    public QuestDTO() {
    }
    public QuestDTO(Quest quest) {
        this.uuid = quest.getUuid();
        this.creatorUuid = quest.getCreatorUuid();
        this.title = quest.getTitle();
        this.description = quest.getDescription();
        this.likes = quest.getLikes();
        this.dislikes = quest.getDislikes();
        this.createdAt = quest.getCreatedAt();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public void setCreatorUuid(UUID creatorUuid) {
        this.creatorUuid = creatorUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isDisliked() {
        return isDisliked;
    }

    public void setDisliked(boolean disliked) {
        isDisliked = disliked;
    }
}
