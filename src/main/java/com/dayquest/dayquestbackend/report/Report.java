package com.dayquest.dayquestbackend.report;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.util.UUID;

@Entity
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = true)
    private String description;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private UUID userId;

    private Type type;

    private ReportStatus status = ReportStatus.PENDING;

    private String modMessage;

    public String getModMessage() {
        return modMessage;
    }

    public void setModMessage(String modMessage) {
        this.modMessage = modMessage;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getEntityUuid() {
        return entityId;
    }

    public void setEntityUuid(UUID videoUuid) {
        this.entityId = videoUuid;
    }

    public UUID getUserUuid() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setUserUuid(UUID userUuid) {
        this.userId = userUuid;
    }
}