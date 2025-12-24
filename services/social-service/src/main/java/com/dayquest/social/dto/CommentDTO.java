package com.dayquest.social.dto;

import com.dayquest.social.model.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public class CommentDTO {
    private UUID uuid;
    private String content;
    private UUID videoUuid;
    private UUID userUuid;
    private UUID parentCommentUuid;
    private int likes;
    private int replies;
    private LocalDateTime createdAt;

    public CommentDTO() {}

    public CommentDTO(Comment comment) {
        this.uuid = comment.getUuid();
        this.content = comment.getContent();
        this.videoUuid = comment.getVideoUuid();
        this.userUuid = comment.getUserUuid();
        this.parentCommentUuid = comment.getParentCommentUuid();
        this.likes = comment.getLikes();
        this.replies = comment.getReplies();
        this.createdAt = comment.getCreatedAt();
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
}
