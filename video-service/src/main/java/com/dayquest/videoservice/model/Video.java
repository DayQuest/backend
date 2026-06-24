package com.dayquest.videoservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos", indexes = {
    @Index(name = "idx_video_user", columnList = "userUuid"),
    @Index(name = "idx_video_quest", columnList = "questUuid"),
    @Index(name = "idx_video_status", columnList = "status"),
    @Index(name = "idx_video_created", columnList = "createdAt"),
    @Index(name = "idx_video_status_created", columnList = "status, createdAt DESC"),
    @Index(name = "idx_video_user_status", columnList = "userUuid, status"),
    @Index(name = "idx_video_score", columnList = "score DESC")
})

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Version
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String filePath;

    private String thumbnailPath;

    @Column(nullable = false)
    private UUID userUuid;

    private UUID questUuid;

    private int upVotes = 0;
    private int downVotes = 0;
    private int views = 0;
    private int comments = 0;

    // Pre-calculated score for efficient sorting (upVotes - downVotes)
    private int score = 0;

    // Denormalized data to avoid joins
    private String uploaderUsername;
    private String questTitle;

    // Soft delete support
    @Column(nullable = false)
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.PROCESSING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityLevel securityLevel = SecurityLevel.NORMAL;

    private float duration;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Getters and Setters
























































































    /**
     * Recalculate score based on upvotes and downvotes.
     */
    public void recalculateScore() {
        this.score = this.upVotes - this.downVotes;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateScore();
    }
}

