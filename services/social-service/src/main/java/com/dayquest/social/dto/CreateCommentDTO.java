package com.dayquest.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateCommentDTO {

    @NotBlank(message = "Content is required")
    @Size(max = 500, message = "Comment must be less than 500 characters")
    private String content;

    private UUID parentCommentUuid;

    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public UUID getParentCommentUuid() { return parentCommentUuid; }
    public void setParentCommentUuid(UUID parentCommentUuid) { this.parentCommentUuid = parentCommentUuid; }
}
