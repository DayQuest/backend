package com.dayquest.common.events;

import java.util.UUID;

/**
 * Event published when a quest is completed by a user.
 */
public class QuestCompletedEvent {
    private UUID questId;
    private UUID userId;
    private UUID videoId;

    public QuestCompletedEvent() {}

    public QuestCompletedEvent(UUID questId, UUID userId, UUID videoId) {
        this.questId = questId;
        this.userId = userId;
        this.videoId = videoId;
    }

    public UUID getQuestId() { return questId; }
    public void setQuestId(UUID questId) { this.questId = questId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }
}
