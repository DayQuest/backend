package com.dayquest.user.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for user profile response.
 */
public class ProfileDTO {
    private UUID uuid;
    private String username;
    private String profilePictureUrl;
    private int followers;
    private int following;
    private boolean isFollowing;
    private List<UUID> badges;
    private UUID dailyQuestId;
    private boolean banned;

    public ProfileDTO() {}

    public ProfileDTO(UUID uuid, String username, String profilePictureUrl, int followers, 
                      int following, boolean isFollowing, List<UUID> badges, 
                      UUID dailyQuestId, boolean banned) {
        this.uuid = uuid;
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
        this.followers = followers;
        this.following = following;
        this.isFollowing = isFollowing;
        this.badges = badges;
        this.dailyQuestId = dailyQuestId;
        this.banned = banned;
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }

    public int getFollowing() { return following; }
    public void setFollowing(int following) { this.following = following; }

    public boolean isFollowing() { return isFollowing; }
    public void setFollowing(boolean following) { isFollowing = following; }

    public List<UUID> getBadges() { return badges; }
    public void setBadges(List<UUID> badges) { this.badges = badges; }

    public UUID getDailyQuestId() { return dailyQuestId; }
    public void setDailyQuestId(UUID dailyQuestId) { this.dailyQuestId = dailyQuestId; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
