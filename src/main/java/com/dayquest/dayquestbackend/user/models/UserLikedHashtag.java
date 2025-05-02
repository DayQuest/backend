package com.dayquest.dayquestbackend.user.models;

import com.dayquest.dayquestbackend.hashtag.Hashtag;
import com.dayquest.dayquestbackend.user.ids.UserLikedHashtagId;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "hashtag_id"})
})
public class UserLikedHashtag {
    @EmbeddedId
    private UserLikedHashtagId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("hashtagId")
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

    @CreationTimestamp
    private LocalDateTime firstLikedTimestamp;

    @CreationTimestamp
    private LocalDateTime lastLikedTimestamp;

    public UserLikedHashtag() {}
    public UserLikedHashtag(User user, Hashtag hashtag) {
        this.user = user;
        this.hashtag = hashtag;
        this.id = new UserLikedHashtagId(user.getUuid(), hashtag.getUuid());
    }

    public UserLikedHashtagId getId() {
        return id;
    }

    public void setId(UserLikedHashtagId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Hashtag getHashtag() {
        return hashtag;
    }

    public void setHashtag(Hashtag hashtag) {
        this.hashtag = hashtag;
    }

    public LocalDateTime getFirstLikedTimestamp() {
        return firstLikedTimestamp;
    }

    public void setFirstLikedTimestamp(LocalDateTime firstLikedTimestamp) {
        this.firstLikedTimestamp = firstLikedTimestamp;
    }

    public LocalDateTime getLastLikedTimestamp() {
        return lastLikedTimestamp;
    }

    public void setLastLikedTimestamp(LocalDateTime lastLikedTimestamp) {
        this.lastLikedTimestamp = lastLikedTimestamp;
    }
}
