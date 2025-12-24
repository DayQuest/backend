package com.dayquest.quest.dto;

import com.dayquest.quest.model.Quest;

import java.time.LocalDateTime;
import java.util.UUID;

public class QuestResponseDTO {
    private UUID uuid;
    private String title;
    private String description;
    private UUID creatorUuid;
    private int likes;
    private int dislikes;
    private double rating;
    private LocalDateTime createdAt;

    public QuestResponseDTO() {}

    public QuestResponseDTO(Quest quest) {
        this.uuid = quest.getUuid();
        this.title = quest.getTitle();
        this.description = quest.getDescription();
        this.creatorUuid = quest.getCreatorUuid();
        this.likes = quest.getLikes();
        this.dislikes = quest.getDislikes();
        this.rating = quest.getRating();
        this.createdAt = quest.getCreatedAt();
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getCreatorUuid() { return creatorUuid; }
    public void setCreatorUuid(UUID creatorUuid) { this.creatorUuid = creatorUuid; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
