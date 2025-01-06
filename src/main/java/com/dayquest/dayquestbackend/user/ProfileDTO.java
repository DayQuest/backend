package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.video.Video;
import com.dayquest.dayquestbackend.video.VideoDTO;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfileDTO {
    private String username;
    private String profilePicture;
    private List<VideoDTO> videos = new ArrayList<>();
    private Quest quest;
    private boolean isFollowing = false;
    private int followers;
    private boolean isBanned;
    private List<UUID> badges;

    public ProfileDTO(String username, String profilePicture, List<Video> videos, Quest quest, boolean isBanned, int followers, boolean isFollowing, List<UUID> badges) {
        this.username = username;
        this.profilePicture = profilePicture;
        this.videos = new ArrayList<>();
        this.quest = quest;
        this.isBanned = isBanned;
        this.followers = followers;
        this.isFollowing = isFollowing;
        this.badges = badges;
        if (videos != null) {
            for(Video video : videos) {
                this.videos.add(new VideoDTO(
                        video.getTitle(),
                        video.getDescription(),
                        video.getUpVotes(),
                        video.getDownVotes(),
                        video.getUser().getUsername(),
                        video.getFilePath(),
                        "http://77.90.21.53:8010/api/videos/thumbnail/" + video.getUuid().toString(),
                        quest,
                        video.getUuid(),
                        video.getCreatedAt(),
                        false
                ));
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

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }
}
