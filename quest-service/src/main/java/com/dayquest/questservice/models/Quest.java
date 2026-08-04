package com.dayquest.questservice.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Quest {
    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;
    private UUID creatorUuid;
    private String title;
    private String description;
    private int likes;
    private int dislikes;
    private String creatorUsername;
    private int score;
    private int videoCount;
    private LocalDateTime createdAt;
    private boolean active;
    private boolean deleted;
    private LocalDateTime deletedAt;
}
