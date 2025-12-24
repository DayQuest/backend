package com.dayquest.common.events;

import java.util.UUID;

/**
 * Event published when a comment is created.
 */
public class CommentCreatedEvent {
    private UUID commentId;
    private UUID videoId;
    private UUID userId;
    private String content;

    public CommentCreatedEvent() {}

    public CommentCreatedEvent(UUID commentId, UUID videoId, UUID userId, String content) {
        this.commentId = commentId;
        this.videoId = videoId;
        this.userId = userId;
        this.content = content;
    }

    public UUID getCommentId() { return commentId; }
    public void setCommentId(UUID commentId) { this.commentId = commentId; }
    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
