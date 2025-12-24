package com.dayquest.quest.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quests")
public class Quest {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private UUID creatorUuid;

    private int likes;
    private int dislikes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean active = true;

    // Constructors
    public Quest() {}

    public Quest(String title, String description, UUID creatorUuid) {
        this.title = title;
        this.description = description;
        this.creatorUuid = creatorUuid;
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getRating() {
        int total = likes + dislikes;
        if (total == 0) return 0.5;
        return (double) likes / total;
    }
}
