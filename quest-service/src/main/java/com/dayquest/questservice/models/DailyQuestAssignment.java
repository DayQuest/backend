package com.dayquest.questservice.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks daily quest assignments for users.
 * Stored in quest-service to keep quest-related data together.
 */
@Entity
@Table(name = "daily_quest_assignment",
    indexes = {
        @Index(name = "idx_daily_quest_user_date", columnList = "userId, assignedDate"),
        @Index(name = "idx_daily_quest_quest", columnList = "questId")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_quest_user_date", columnNames = {"userId", "assignedDate"})
    }
)

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class DailyQuestAssignment {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID questId;

    @Column(nullable = false)
    private LocalDate assignedDate;

    @Column(nullable = false)
    @lombok.Builder.Default
    private int rerollsUsed = 0;

    @Column(nullable = false)
    @lombok.Builder.Default
    private int maxRerolls = 3;

    @Column(nullable = false)
    @lombok.Builder.Default
    private boolean completed = false;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean canReroll() {
        return rerollsUsed < maxRerolls;
    }

    public void incrementRerolls() {
        this.rerollsUsed++;
    }
}


