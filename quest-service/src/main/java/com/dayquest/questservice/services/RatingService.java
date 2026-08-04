package com.dayquest.questservice.services;

import com.dayquest.questservice.models.QuestRating;
import com.dayquest.questservice.repositories.QuestRatingRepository;
import com.dayquest.questservice.repositories.QuestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);

    @Autowired
    private QuestRatingRepository questRatingRepository;

    @Autowired
    private QuestRepository questRepository;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public RatingService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Get all quest IDs liked by user
     */
    @Cacheable(value = "userLikedQuests", key = "#userId")
    public Set<UUID> getLikedQuestIds(UUID userId) {
        return new HashSet<>(questRatingRepository.findLikedQuestIdsByUserId(userId));
    }

    /**
     * Get all quest IDs disliked by user
     */
    @Cacheable(value = "userDislikedQuests", key = "#userId")
    public Set<UUID> getDislikedQuestIds(UUID userId) {
        return new HashSet<>(questRatingRepository.findDislikedQuestIdsByUserId(userId));
    }

    /**
     * Rate a quest (like or dislike) with toggle functionality
     */
    @Async
    @CacheEvict(value = {"userLikedQuests", "userDislikedQuests", "quest", "quests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> rateQuest(UUID userId, UUID questId, boolean isLike) {
        return CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> {
            try {
                // Check if quest exists
                if (!questRepository.existsById(questId)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Quest not found");
                }

                Optional<QuestRating> existingRatingOpt = questRatingRepository.findByUserIdAndQuestId(userId, questId);

                if (existingRatingOpt.isPresent()) {
                    QuestRating existingRating = existingRatingOpt.get();

                    if (existingRating.isLike() == isLike) {
                        // Toggle off: User clicked same rating again -> Remove it
                        questRatingRepository.delete(existingRating);

                        if (isLike) {
                            questRepository.decrementLikes(questId);
                        } else {
                            questRepository.decrementDislikes(questId);
                        }

                        logger.info("User {} removed {} from quest {}", userId, isLike ? "like" : "dislike", questId);
                        return ResponseEntity.ok("Rating removed");
                    } else {
                        // Switch rating: User clicked different rating -> Update it
                        // e.g. was Like, now Dislike
                        if (isLike) {
                            // Was dislike, becoming like
                            questRepository.decrementDislikes(questId);
                            questRepository.incrementLikes(questId);
                        } else {
                            // Was like, becoming dislike
                            questRepository.decrementLikes(questId);
                            questRepository.incrementDislikes(questId);
                        }

                        existingRating.setLike(isLike);
                        questRatingRepository.save(existingRating);
                        logger.info("User {} changed rating on quest {} to {}", userId, questId, isLike ? "like" : "dislike");
                        return ResponseEntity.ok(isLike ? "Liked" : "Disliked");
                    }
                } else {
                    // New rating
                    QuestRating newRating = QuestRating.builder()
                            .userId(userId)
                            .questId(questId)
                            .isLike(isLike)
                            .build();
                    questRatingRepository.save(newRating);

                    if (isLike) {
                        questRepository.incrementLikes(questId);
                    } else {
                        questRepository.incrementDislikes(questId);
                    }

                    logger.info("User {} {} quest {}", userId, isLike ? "liked" : "disliked", questId);
                    return ResponseEntity.ok(isLike ? "Liked" : "Disliked");
                }

            } catch (Exception e) {
                logger.error("Error rating quest {} by user {}", questId, userId, e);
                // Mark transaction for rollback
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error rating quest");
            }
        }));
    }

    /**
     * Remove rating from a quest
     */
    @Async
    @CacheEvict(value = {"userLikedQuests", "userDislikedQuests", "quest", "quests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> removeRating(UUID userId, UUID questId) {
        return CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> {
            try {
                Optional<QuestRating> existingRating = questRatingRepository.findByUserIdAndQuestId(userId, questId);

                if (existingRating.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rating not found");
                }

                QuestRating rating = existingRating.get();
                boolean wasLike = rating.isLike();

                questRatingRepository.delete(rating);

                // Decrement the appropriate counter
                if (wasLike) {
                    questRepository.decrementLikes(questId);
                } else {
                    questRepository.decrementDislikes(questId);
                }

                logger.info("User {} removed {} from quest {}", userId, wasLike ? "like" : "dislike", questId);
                return ResponseEntity.ok("Rating removed");

            } catch (Exception e) {
                logger.error("Error removing rating from quest {} by user {}", questId, userId, e);
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing rating");
            }
        }));
    }

    /**
     * Check if user has rated a quest
     */
    public boolean hasRated(UUID userId, UUID questId) {
        return questRatingRepository.existsByUserIdAndQuestId(userId, questId);
    }

    /**
     * Get user's rating for a quest
     */
    public Optional<Boolean> getUserRating(UUID userId, UUID questId) {
        return questRatingRepository.findByUserIdAndQuestId(userId, questId)
                .map(QuestRating::isLike);
    }
}

