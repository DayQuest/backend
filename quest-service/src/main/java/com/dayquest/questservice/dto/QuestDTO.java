package com.dayquest.questservice.dto;
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
public class QuestDTO {
    private UUID uuid;
    private UUID creatorUuid;
    private String creatorUsername;
    private String title;
    private String description;
    private int likes;
    private int dislikes;
    private int score;
    private int videoCount;
    private LocalDateTime createdAt;
    private boolean liked;
    private boolean disliked;
}
