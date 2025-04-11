package com.dayquest.dayquestbackend.hashtag;

import com.dayquest.dayquestbackend.video.models.Video;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
public class Hashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;
    private String hashtag;
    private int videoCount;

    @ManyToMany(mappedBy = "hashtags")
    private List<Video> videos;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }
}
