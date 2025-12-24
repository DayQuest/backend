package com.dayquest.social.dto;

import com.dayquest.social.model.Notification;
import com.dayquest.social.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDTO {
    private UUID uuid;
    private UUID actorUuid;
    private NotificationType type;
    private String message;
    private UUID referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public NotificationDTO(Notification notification) {
        this.uuid = notification.getUuid();
        this.actorUuid = notification.getActorUuid();
        this.type = notification.getType();
        this.message = notification.getMessage();
        this.referenceId = notification.getReferenceId();
        this.read = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

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
