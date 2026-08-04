package com.dayquest.questservice.listener;

import com.dayquest.common.events.UserDeletedEvent;
import com.dayquest.common.messaging.RabbitMQConstants;
import com.dayquest.questservice.repositories.DailyQuestAssignmentRepository;
import com.dayquest.questservice.repositories.QuestRatingRepository;
import com.dayquest.questservice.repositories.QuestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens for user deleted events and cleans up all user-related quest data.
 */
@Component
public class UserDeletedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletedEventListener.class);

    private final QuestRepository questRepository;
    private final QuestRatingRepository questRatingRepository;
    private final DailyQuestAssignmentRepository dailyQuestAssignmentRepository;

    public UserDeletedEventListener(QuestRepository questRepository,
                                    QuestRatingRepository questRatingRepository,
                                    DailyQuestAssignmentRepository dailyQuestAssignmentRepository) {
        this.questRepository = questRepository;
        this.questRatingRepository = questRatingRepository;
        this.dailyQuestAssignmentRepository = dailyQuestAssignmentRepository;
    }

    @RabbitListener(queues = RabbitMQConstants.USER_DELETED_QUEST_QUEUE)
    @Transactional
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        logger.info("Received UserDeletedEvent for user: {} ({})", event.getUsername(), event.getUserId());

        try {
            // 1. Soft delete all quests created by this user
            int deletedQuests = questRepository.softDeleteByCreatorUuid(event.getUserId());
            logger.info("Soft-deleted {} quests for user: {}", deletedQuests, event.getUserId());

            // 2. Delete all quest ratings (likes/dislikes) by this user
            questRatingRepository.deleteByUserId(event.getUserId());
            logger.info("Deleted quest ratings for user: {}", event.getUserId());

            // 3. Delete all daily quest assignments for this user
            dailyQuestAssignmentRepository.deleteByUserId(event.getUserId());
            logger.info("Deleted daily quest assignments for user: {}", event.getUserId());

            logger.info("Successfully cleaned up all quest data for user: {} ({})",
                    event.getUsername(), event.getUserId());

        } catch (Exception e) {
            logger.error("Error cleaning up quest data for user: {} - {}", event.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }
}

