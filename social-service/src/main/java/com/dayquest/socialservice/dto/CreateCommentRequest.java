package com.dayquest.socialservice.dto;

import com.dayquest.socialservice.model.CommentEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must be less than 2000 characters")
    private String content;

    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    @NotNull(message = "Entity type is required")
    private CommentEntityType entityType;

    private UUID parentCommentId;
}
