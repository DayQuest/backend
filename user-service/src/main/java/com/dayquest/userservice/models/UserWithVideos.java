package com.dayquest.userservice.models;

import com.dayquest.userservice.dto.VideoDTO;
import com.dayquest.userservice.enums.Punishments;

import java.time.LocalDateTime;
import java.util.*;


public class UserWithVideos{

    private UUID uuid;

    private LocalDateTime createdAt;

    private String username;

    private String email;

    private Punishments punishment;

    private List<UUID> badges;

    private int followers;

    private List<VideoDTO> videos;

    private byte[] profilePicture;


    public UUID getUuid() {
        return uuid;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public Punishments getPunishment() {
        return punishment;
    }

    public void setPunishment(Punishments punishment) {
        this.punishment = punishment;
    }

    public List<UUID> getBadges() {
        return badges;
    }

    public void setBadges(List<UUID> badges) {
        this.badges = badges;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public List<VideoDTO> getVideos() {
        return videos;
    }

    public void setVideos(List<VideoDTO> videos) {
        this.videos = videos;
    }
}

