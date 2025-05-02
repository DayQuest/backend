package com.dayquest.dayquestbackend.user.models;

import com.dayquest.dayquestbackend.user.ids.VideoRatingId;
import com.dayquest.dayquestbackend.video.models.Video;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id"}))
public class VideoRating {

    @EmbeddedId
    private VideoRatingId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("videoId")
    @JoinColumn(name = "video_id")
    private Video video;

    private boolean liked;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public VideoRating() {}
    public VideoRating(User user, Video video, boolean liked) {
        this.user = user;
        this.video = video;
        this.liked = liked;
        this.id = new VideoRatingId(user.getUuid(), video.getUuid());
    }

    public VideoRatingId getId() {
        return id;
    }

    public void setId(VideoRatingId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
