package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.video.Video;

import java.util.List;

public class ProfileDTO {
    private String username;
    private byte[] profilePicture;
    private List<Video> videos;

    public ProfileDTO(String username, byte[] profilePicture, List<Video> videos) {
        this.username = username;
        this.profilePicture = profilePicture;
        this.videos = videos;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }

}
