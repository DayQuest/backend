package com.dayquest.dayquestbackend.comment.dto;

import java.util.UUID;

public class CommentDTO {
    private String content;
    private UUID userId;
    private UUID videoId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }
}
