package com.dayquest.userservice.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfileDTO {
    private String username;
    private String profilePicture;
    private List<VideoDTO> videos = new ArrayList<>();
    private boolean isFollowing = false;
    private int followers;
    private boolean isBanned;
    private List<UUID> badges;

    public ProfileDTO(String username, String profilePicture, List<VideoDTO> videos, boolean isBanned, int followers, boolean isFollowing, List<UUID> badges) {
        this.username = username;
        this.profilePicture = profilePicture;
        this.videos = new ArrayList<>();
        this.isBanned = isBanned;
        this.followers = followers;
        this.isFollowing = isFollowing;
        this.badges = badges;
        if (videos != null) {
            for(VideoDTO video : videos) {
                this.videos.add(video);
            }
        }
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public List<VideoDTO> getVideos() {
        return videos;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setVideos(List<VideoDTO> videos) {
        this.videos = videos;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public List<UUID> getBadges() {
        return badges;
    }

    public void setBadges(List<UUID> badges) {
        this.badges = badges;
    }
}
