package com.dayquest.questservice.services;

import com.dayquest.questservice.models.DailyQuestAssignment;
import com.dayquest.questservice.models.Quest;
import com.dayquest.questservice.repositories.DailyQuestAssignmentRepository;
import com.dayquest.questservice.repositories.QuestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class QuestService {

    private static final Logger logger = LoggerFactory.getLogger(QuestService.class);
    private static final Random RANDOM = new Random();

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private DailyQuestAssignmentRepository dailyQuestAssignmentRepository;

    /**
     * Create a new quest
     */
    @Async
    @Transactional
    @CacheEvict(value = {"quests", "topQuests"}, allEntries = true)
    public CompletableFuture<Quest> createQuest(String title, String description, UUID creatorId, String creatorUsername) {
        return CompletableFuture.supplyAsync(() -> {
            if (title == null || description == null || title.isBlank() || description.isBlank()) {
                logger.warn("Invalid quest creation attempt: title or description is blank");
                return null;
            }

            Quest quest = new Quest();
            quest.setTitle(title.trim());
            quest.setDescription(description.trim());
            quest.setCreatorUuid(creatorId);
            quest.setCreatorUsername(creatorUsername);

            questRepository.save(quest);
            logger.info("Quest created: {} by user {} ({})", quest.getUuid(), creatorUsername, creatorId);
            return quest;
        });
    }

    /**
     * Get top 30% of quests by score using efficient pagination.
     */
    @Async
    @Cacheable(value = "topQuests", unless = "#result == null")
    public CompletableFuture<List<Quest>> getTop30PercentQuests() {
        return CompletableFuture.supplyAsync(() -> {
            long totalCount = questRepository.countActiveQuests();
            if (totalCount == 0) {
                logger.debug("No active quests found");
                return List.of();
            }
            int topCount = Math.max(1, (int) Math.ceil(totalCount * 0.3));
            int limit = Math.min(topCount, 100);
            logger.debug("Fetching top {} quests from {} total", limit, totalCount);
            return questRepository.findTopQuestsByScore(PageRequest.of(0, limit)).getContent();
        });
    }

    /**
     * Get a random quest from the top quests
     */
    @Async
    public CompletableFuture<Quest> getRandomQuest() {
        return CompletableFuture.supplyAsync(() -> {
            long totalCount = questRepository.countActiveQuests();
            if (totalCount == 0) {
                return null;
            }

            // Get from top 50% to ensure quality
            int topCount = Math.max(1, (int) Math.ceil(totalCount * 0.5));
            int limit = Math.min(topCount, 100);

            Page<Quest> topQuests = questRepository.findTopQuestsByScore(PageRequest.of(0, limit));
            List<Quest> quests = topQuests.getContent();

            if (quests.isEmpty()) {
                return null;
            }

            return quests.get(RANDOM.nextInt(quests.size()));
        });
    }

    /**
     * Get daily quest for user - either existing assignment or create new one
     */
    @Async
    @Transactional
    public CompletableFuture<Quest> getDailyQuestForUser(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate today = LocalDate.now();

            // Check for existing assignment
            Optional<DailyQuestAssignment> existingAssignment =
                    dailyQuestAssignmentRepository.findByUserIdAndAssignedDate(userId, today);

            if (existingAssignment.isPresent()) {
                UUID questId = existingAssignment.get().getQuestId();
                return questRepository.findByUuidAndDeletedFalse(questId).orElse(null);
            }

            // Create new assignment
            Quest randomQuest = getRandomQuest().join();
            if (randomQuest == null) {
                logger.warn("No quests available for daily assignment to user {}", userId);
                return null;
            }

            DailyQuestAssignment assignment = DailyQuestAssignment.builder()
                    .userId(userId)
                    .questId(randomQuest.getUuid())
                    .assignedDate(today)
                    .build();
            dailyQuestAssignmentRepository.save(assignment);

            logger.info("Assigned daily quest {} to user {}", randomQuest.getUuid(), userId);
            return randomQuest;
        });
    }

    /**
     * Reroll daily quest - get a new random quest if rerolls available
     */
    @Async
    @Transactional
    public CompletableFuture<Quest> rerollDailyQuest(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate today = LocalDate.now();

            Optional<DailyQuestAssignment> existingAssignment =
                    dailyQuestAssignmentRepository.findByUserIdAndAssignedDate(userId, today);

            if (existingAssignment.isEmpty()) {
                return getDailyQuestForUser(userId).join();
            }

            DailyQuestAssignment assignment = existingAssignment.get();

            // Check if rerolls available
            if (!assignment.canReroll()) {
                logger.info("User {} has no rerolls remaining", userId);
                return null;
            }

            // Get a different random quest
            UUID currentQuestId = assignment.getQuestId();
            Quest newQuest = null;
            int attempts = 0;

            while (attempts < 5) {
                Quest candidate = getRandomQuest().join();
                if (candidate != null && !candidate.getUuid().equals(currentQuestId)) {
                    newQuest = candidate;
                    break;
                }
                attempts++;
            }

            if (newQuest == null) {
                // Fallback to any random quest
                newQuest = getRandomQuest().join();
            }

            if (newQuest == null) {
                logger.warn("No quests available for reroll for user {}", userId);
                return null;
            }

            // Update assignment
            assignment.setQuestId(newQuest.getUuid());
            assignment.incrementRerolls();
            dailyQuestAssignmentRepository.save(assignment);

            logger.info("User {} rerolled daily quest to {} (rerolls used: {})",
                    userId, newQuest.getUuid(), assignment.getRerollsUsed());

            return newQuest;
        });
    }

    /**
     * Get remaining rerolls for a user's daily quest
     */
    @Async
    public CompletableFuture<Map<String, Integer>> getRemainingRerolls(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate today = LocalDate.now();
            Optional<DailyQuestAssignment> assignment =
                    dailyQuestAssignmentRepository.findByUserIdAndAssignedDate(userId, today);

            if (assignment.isEmpty()) {
                return Map.of("rerollsUsed", 0, "rerollsRemaining", 3, "maxRerolls", 3);
            }

            DailyQuestAssignment dailyQuest = assignment.get();
            int used = dailyQuest.getRerollsUsed();
            int max = dailyQuest.getMaxRerolls();
            return Map.of("rerollsUsed", used, "rerollsRemaining", max - used, "maxRerolls", max);
        });
    }

    /**
     * Mark daily quest as completed
     */
    @Async
    @Transactional
    public CompletableFuture<Boolean> completeDailyQuest(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate today = LocalDate.now();

            Optional<DailyQuestAssignment> assignment =
                    dailyQuestAssignmentRepository.findByUserIdAndAssignedDate(userId, today);

            if (assignment.isEmpty()) {
                return false;
            }

            DailyQuestAssignment dailyQuest = assignment.get();
            if (dailyQuest.isCompleted()) {
                return true; // Already completed
            }

            dailyQuest.setCompleted(true);
            dailyQuest.setCompletedAt(java.time.LocalDateTime.now());
            dailyQuestAssignmentRepository.save(dailyQuest);

            logger.info("User {} completed daily quest {}", userId, dailyQuest.getQuestId());
            return true;
        });
    }

    /**
     * Increment video count for a quest
     */
    @Transactional
    @CacheEvict(value = {"quest", "quests"}, allEntries = true)
    public void incrementVideoCount(UUID questId) {
        questRepository.incrementVideoCount(questId);
    }

    /**
     * Decrement video count for a quest
     */
    @Transactional
    @CacheEvict(value = {"quest", "quests"}, allEntries = true)
    public void decrementVideoCount(UUID questId) {
        questRepository.decrementVideoCount(questId);
    }

    /**
     * Delete a quest (soft delete)
     */
    @Transactional
    @CacheEvict(value = {"quests", "quest", "topQuests"}, allEntries = true)
    public boolean deleteQuest(UUID questId, UUID userId) {
        Optional<Quest> questOpt = questRepository.findByUuidAndDeletedFalse(questId);

        if (questOpt.isEmpty()) {
            return false;
        }

        Quest quest = questOpt.get();
        // Only creator can delete (admin check removed for now as per controller logic)
        if (!quest.getCreatorUuid().equals(userId)) {
            throw new RuntimeException("Only creator can delete quest");
        }

        questRepository.softDelete(questId);
        logger.info("Quest {} deleted by user {}", questId, userId);
        return true;
    }
}
