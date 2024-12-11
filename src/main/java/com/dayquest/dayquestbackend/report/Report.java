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
    private UUID entityUuid;

    @Column(nullable = false)
    private UUID userUuid;

    private Type type;

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
        return entityUuid;
    }

    public void setEntityUuid(UUID videoUuid) {
        this.entityUuid = videoUuid;
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }
}