package com.dayquest.socialservice.listener;

import com.dayquest.common.events.UserDeletedEvent;
import com.dayquest.common.messaging.RabbitMQConstants;
import com.dayquest.socialservice.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens for user deleted events and cleans up all user-related social data (comments, etc.).
 */
@Component
public class UserDeletedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletedEventListener.class);

    private final CommentRepository commentRepository;

    public UserDeletedEventListener(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @RabbitListener(queues = RabbitMQConstants.USER_DELETED_SOCIAL_QUEUE)
    @Transactional
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        logger.info("Received UserDeletedEvent for user: {} ({})", event.getUsername(), event.getUserId());

        try {
            // Delete all comments made by this user
            commentRepository.deleteByUserUuid(event.getUserId());
            logger.info("Deleted all comments for user: {}", event.getUserId());

            logger.info("Successfully cleaned up all social data for user: {} ({})",
                    event.getUsername(), event.getUserId());

        } catch (Exception e) {
            logger.error("Error cleaning up social data for user: {} - {}", event.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }
}

