package com.dayquest.contentservice.model;

import jakarta.persistence.*;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "streaks", indexes = {
    @Index(name = "idx_streak_user", columnList = "userUuid"),
    @Index(name = "idx_streak_current", columnList = "currentStreak DESC"),
    @Index(name = "idx_streak_longest", columnList = "longestStreak DESC"),
    @Index(name = "idx_streak_last_activity", columnList = "lastActivityDate")
})

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userUuid;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private int longestStreak = 0;

    private LocalDateTime lastActivityDate;

    @CreationTimestamp
    private LocalDateTime createdAt;


    // Getters and Setters























}

