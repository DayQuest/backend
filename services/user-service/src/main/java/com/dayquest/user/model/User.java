package com.dayquest.user.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User entity for the User microservice.
 * This is a simplified version focused on user profile data.
 * Authentication-related fields are managed by the Auth Service.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    private int interactions;

    private int followers;

    private int following;

    @Lob
    @Column
    private byte[] profilePicture;

    @ElementCollection
    @CollectionTable(name = "user_badges", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> badges = new ArrayList<>();

    private UUID dailyQuestId;

    @ElementCollection
    @CollectionTable(name = "user_done_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> doneQuests = new ArrayList<>();

    private int leftRerolls;
    private LocalDateTime lastReroll;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private UserStatus status = UserStatus.ACTIVE;

    private String adminComment;
    
    private LocalDateTime lastLogin;

    // Constructors
    public User() {}

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.status = UserStatus.ACTIVE;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getInteractions() { return interactions; }
    public void setInteractions(int interactions) { this.interactions = interactions; }
    public void increaseInteractions() { this.interactions++; }

    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }

    public int getFollowing() { return following; }
    public void setFollowing(int following) { this.following = following; }

    public byte[] getProfilePicture() { return profilePicture; }
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; }

    public List<UUID> getBadges() { return badges; }
    public void setBadges(List<UUID> badges) { this.badges = badges; }

    public UUID getDailyQuestId() { return dailyQuestId; }
    public void setDailyQuestId(UUID dailyQuestId) { this.dailyQuestId = dailyQuestId; }

    public List<UUID> getDoneQuests() { return doneQuests; }
    public void setDoneQuests(List<UUID> doneQuests) { this.doneQuests = doneQuests; }
    public void addDoneQuest(UUID questId) { this.doneQuests.add(questId); }

    public int getLeftRerolls() { return leftRerolls; }
    public void setLeftRerolls(int leftRerolls) { this.leftRerolls = leftRerolls; }

    public LocalDateTime getLastReroll() { return lastReroll; }
    public void setLastReroll(LocalDateTime lastReroll) { this.lastReroll = lastReroll; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}
