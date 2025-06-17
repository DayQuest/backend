package com.dayquest.dayquestbackend.badge;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Badge {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    private String name;
    private String description;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] image;

    @ElementCollection
    @CollectionTable(name = "badge_user_ids", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "user_id")
    private List<UUID> userIds = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID uuid) {
        this.id = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public List<UUID> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<UUID> userIds) {
        this.userIds = userIds;
    }
}