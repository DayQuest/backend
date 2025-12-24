package com.dayquest.social.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    private UUID userUuid;
    private UUID actorUuid;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;
    private UUID referenceId;

    private boolean read;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructors
    public Notification() {}

    public Notification(UUID userUuid, UUID actorUuid, NotificationType type, String message, UUID referenceId) {
        this.userUuid = userUuid;
        this.actorUuid = actorUuid;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.read = false;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }

    public UUID getActorUuid() { return actorUuid; }
    public void setActorUuid(UUID actorUuid) { this.actorUuid = actorUuid; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
