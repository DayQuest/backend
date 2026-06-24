package com.dayquest.videoservice.dto;
import com.dayquest.videoservice.model.SecurityLevel;
import com.dayquest.videoservice.model.VideoStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDTO {
    private UUID uuid;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private UUID userUuid;
    private String username;
    private UUID questUuid;
    private int upVotes;
    private int downVotes;
    private int views;
    private int comments;
    private VideoStatus status;
    private SecurityLevel securityLevel;
    private float duration;
    private LocalDateTime createdAt;
    private boolean isLikedByUser;
    private boolean isDislikedByUser;
}
