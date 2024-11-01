package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.video.Video;
import com.dayquest.dayquestbackend.video.VideoDTO;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class ProfileDTO {
    private String username;
    private String profilePicture;
    private List<VideoDTO> videos = new ArrayList<>();

    public ProfileDTO(String username, String profilePicture, List<Video> videos) {
        this.username = username;
        this.profilePicture = profilePicture;
        this.videos = new ArrayList<>();
        if (videos != null) {
            for(Video video : videos) {
                this.videos.add(new VideoDTO(
                        video.getTitle(),
                        video.getDescription(),
                        video.getUpVotes(),
                        video.getDownVotes(),
                        video.getUser().getUsername(),
                        video.getFilePath(),
                        video.getThumbnail()
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


    public List<VideoDTO> getVideos() {
        return videos;
    }
}
