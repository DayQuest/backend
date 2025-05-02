package com.dayquest.dayquestbackend.user.ids;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class QuestRatingId implements Serializable {

    private UUID userId;
    private UUID questId;

    public QuestRatingId() {}

    public QuestRatingId(UUID user, UUID quest) {
        this.userId = user;
        this.questId = quest;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getQuestId() {
        return questId;
    }

    public void setQuestId(UUID questId) {
        this.questId = questId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestRatingId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(questId, that.questId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, questId);
    }
}
