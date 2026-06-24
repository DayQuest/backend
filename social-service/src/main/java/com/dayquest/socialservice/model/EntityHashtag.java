package com.dayquest.socialservice.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "entity_hashtags")
@IdClass(EntityHashtagId.class)
public class EntityHashtag {

    @Id
    @Column(name = "entity_id")
    private UUID entityId;

    @Id
    @Column(name = "hashtag_id")
    private UUID hashtagId;

    @Enumerated(EnumType.STRING)
    private CommentEntityType entityType; // VIDEO or QUEST

    public EntityHashtag() {}

    public EntityHashtag(UUID entityId, UUID hashtagId, CommentEntityType entityType) {
        this.entityId = entityId;
        this.hashtagId = hashtagId;
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getHashtagId() {
        return hashtagId;
    }

    public void setHashtagId(UUID hashtagId) {
        this.hashtagId = hashtagId;
    }

    public CommentEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(CommentEntityType entityType) {
        this.entityType = entityType;
    }
}

