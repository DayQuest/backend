package com.dayquest.questservice.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quest_rating",
    indexes = {
        @Index(name = "idx_quest_rating_user", columnList = "userId"),
        @Index(name = "idx_quest_rating_quest", columnList = "questId"),
        @Index(name = "idx_quest_rating_like", columnList = "userId, isLike")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_quest_rating_user_quest", columnNames = {"userId", "questId"})
    }
)

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class QuestRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID questId;

    @Column(nullable = false)
    private boolean isLike;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}

