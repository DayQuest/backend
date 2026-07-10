package com.dayquest.socialservice.scheduler;

import com.dayquest.socialservice.model.Comment;
import com.dayquest.socialservice.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CommentCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CommentCleanupScheduler.class);

    private final CommentRepository commentRepository;

    public CommentCleanupScheduler(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    /**
     * Runs daily at 3 AM. Hard-deletes comments that were soft-deleted more than 7 days ago.
     * Replies to these parent comments are cascaded automatically via JPA orphan removal
     * (ensure cascade = CascadeType.ALL + orphanRemoval = true on Comment.replies).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeletedComments() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<Comment> toDelete = commentRepository.findByDeletedTrueAndDeletedAtBefore(cutoff);

        if (toDelete.isEmpty()) {
            logger.debug("Comment cleanup: no soft-deleted comments older than 7 days found");
            return;
        }

        logger.info("Comment cleanup: permanently deleting {} comments soft-deleted before {}",
                toDelete.size(), cutoff);
        commentRepository.deleteAll(toDelete);
        logger.info("Comment cleanup complete");
    }
}