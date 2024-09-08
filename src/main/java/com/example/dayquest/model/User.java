package com.example.dayquest.model;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    public List<Long> getDislikedVideos() {
        return dislikedVideos;
    }

    public void setDislikedVideos(List<Long> dislikedVideos) {
        this.dislikedVideos = dislikedVideos;
    }

    @ElementCollection
    @CollectionTable(name = "disliked_videos", joinColumns = @JoinColumn(name = "user_id"))
    private List<Long> dislikedVideos;

    public List<Long> getDislikedQuests() {
        return dislikedQuests;
    }

    public void setDislikedQuests(List<Long> dislikedQuests) {
        this.dislikedQuests = dislikedQuests;
    }

    @ElementCollection
    @CollectionTable(name = "disliked_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<Long> dislikedQuests;

    public List<Long> getLikedQuests() {
        return likedQuests;
    }

    public void setLikedQuests(List<Long> likedQuests) {
        this.likedQuests = likedQuests;
    }

    @ElementCollection
    @CollectionTable(name = "liked_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<Long> likedQuests;

    public List<Long> getLikedVideos() {
        return likedVideos;
    }

    public void setLikedVideos(List<Long> likedVideos) {
        this.likedVideos = likedVideos;
    }

    @ElementCollection
    @CollectionTable(name = "liked_videos", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "video_id")
    private List<Long> likedVideos;


    @ManyToOne
    @JoinColumn(name = "daily_quest_id")
    private Quest dailyQuest;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    private UUID uuid;

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    private boolean banned;

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public Quest getDailyQuest() {
        return dailyQuest;
    }

    public void setDailyQuest(Quest dailyQuest) {
        this.dailyQuest = dailyQuest;
    }
}
