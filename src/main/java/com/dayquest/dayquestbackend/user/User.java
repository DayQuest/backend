package com.dayquest.dayquestbackend.user;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.video.Video;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;


@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean banned;

    @ElementCollection
    @CollectionTable(name = "disliked_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> dislikedQuests;

    @ElementCollection
    @CollectionTable(name = "disliked_videos", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> dislikedVideos;

    @ElementCollection
    @CollectionTable(name = "liked_quests", joinColumns = @JoinColumn(name = "user_id"))
    private List<UUID> likedQuests;

    @ElementCollection
    @CollectionTable(name = "liked_videos", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "video_id")
    private List<UUID> likedVideos;

    @ManyToOne
    @JoinColumn(name = "daily_quest_id")
    private Quest dailyQuest;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Video> postedVideos = new ArrayList<>();

    public User() {
        this.postedVideos = new ArrayList<>();
        this.dislikedQuests = new ArrayList<>();
        this.dislikedVideos = new ArrayList<>();
        this.likedQuests = new ArrayList<>();
        this.likedVideos = new ArrayList<>();
    }

    public void addPostedVideo(Video video) {
        postedVideos.add(video);
        video.setUser(this);
    }

    public List<Video> getPostedVideos() {
        return postedVideos;
    }

    public void setPostedVideos(List<Video> postedVideos) {
        this.postedVideos = postedVideos;
    }


    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public List<UUID> getDislikedQuests() {
        return dislikedQuests;
    }

    public void setDislikedQuests(List<UUID> dislikedQuests) {
        this.dislikedQuests = dislikedQuests;
    }

    public List<UUID> getDislikedVideos() {
        return dislikedVideos;
    }

    public void setDislikedVideos(List<UUID> dislikedVideos) {
        this.dislikedVideos = dislikedVideos;
    }

    public List<UUID> getLikedQuests() {
        return likedQuests;
    }

    public void setLikedQuests(List<UUID> likedQuests) {
        this.likedQuests = likedQuests;
    }

    public List<UUID> getLikedVideos() {
        return likedVideos;
    }

    public void setLikedVideos(List<UUID> likedVideos) {
        this.likedVideos = likedVideos;
    }

    public Quest getDailyQuest() {
        return dailyQuest;
    }

    public void setDailyQuest(Quest dailyQuest) {
        this.dailyQuest = dailyQuest;
    }
}
