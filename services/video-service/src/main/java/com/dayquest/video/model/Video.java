package com.dayquest.video.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String filePath;

    private UUID userUuid;
    private UUID questUuid;

    private int upVotes;
    private int downVotes;
    private int views;
    private int comments;
    private float length;

    @Enumerated(EnumType.ORDINAL)
    private VideoStatus status = VideoStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private SecurityLevel securityLevel = SecurityLevel.NORMAL;

    @ElementCollection
    @CollectionTable(name = "video_hashtags", joinColumns = @JoinColumn(name = "video_id"))
    private List<UUID> hashtagIds = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructors
    public Video() {}

    public Video(String title, String description, UUID userUuid, UUID questUuid) {
        this.title = title;
        this.description = description;
        this.userUuid = userUuid;
        this.questUuid = questUuid;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }

    public UUID getQuestUuid() { return questUuid; }
    public void setQuestUuid(UUID questUuid) { this.questUuid = questUuid; }

    public int getUpVotes() { return upVotes; }
    public void setUpVotes(int upVotes) { this.upVotes = upVotes; }

    public int getDownVotes() { return downVotes; }
    public void setDownVotes(int downVotes) { this.downVotes = downVotes; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }

    public float getLength() { return length; }
    public void setLength(float length) { this.length = length; }

    public VideoStatus getStatus() { return status; }
    public void setStatus(VideoStatus status) { this.status = status; }

    public SecurityLevel getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(SecurityLevel securityLevel) { this.securityLevel = securityLevel; }

    public List<UUID> getHashtagIds() { return hashtagIds; }
    public void setHashtagIds(List<UUID> hashtagIds) { this.hashtagIds = hashtagIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
