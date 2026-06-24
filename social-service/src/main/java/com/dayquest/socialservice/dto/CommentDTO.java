package com.dayquest.socialservice.dto;

import com.dayquest.socialservice.model.CommentEntityType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private UUID id;
    private String content;
    private UUID entityId;
    private CommentEntityType entityType;
    private UUID userUuid;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int likes;
    private UUID parentCommentId;
    private List<CommentDTO> replies;
}
