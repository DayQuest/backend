package com.dayquest.userservice.models;

import com.dayquest.userservice.ids.FollowId;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "followed_id"}))
public class Follow {

    @EmbeddedId
    private FollowId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("followedId")
    @JoinColumn(name = "followed_id")
    private User followed;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public Follow() {}

    public Follow(User user, User followed) {
        this.user = user;
        this.followed = followed;
        this.id = new FollowId(user.getUuid(), followed.getUuid());
    }

    public FollowId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public User getFollowed() {
        return followed;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
