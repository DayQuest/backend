package com.dayquest.social.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Column(nullable = false, length = 500)
    private String content;

    private UUID videoUuid;
    private UUID userUuid;
    private UUID parentCommentUuid;

    private int likes;
    private int replies;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean deleted;

    // Constructors
    public Comment() {}

    public Comment(String content, UUID videoUuid, UUID userUuid) {
        this.content = content;
        this.videoUuid = videoUuid;
        this.userUuid = userUuid;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public UUID getVideoUuid() { return videoUuid; }
    public void setVideoUuid(UUID videoUuid) { this.videoUuid = videoUuid; }

    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }

    public UUID getParentCommentUuid() { return parentCommentUuid; }
    public void setParentCommentUuid(UUID parentCommentUuid) { this.parentCommentUuid = parentCommentUuid; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getReplies() { return replies; }
    public void setReplies(int replies) { this.replies = replies; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
