package com.dayquest.dayquestbackend.user.models;

import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.user.ids.QuestRatingId;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
public class QuestRating {

    @EmbeddedId
    private QuestRatingId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("questId")
    @JoinColumn(name = "quest_id")
    private Quest quest;

    private boolean liked;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public QuestRating() {}

    public QuestRating(User user, Quest quest, boolean liked) {
        this.user = user;
        this.quest = quest;
        this.liked = liked;
        this.id = new QuestRatingId(user.getUuid(), quest.getUuid());
    }

    public QuestRatingId getId() {
        return id;
    }

    public void setId(QuestRatingId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
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
