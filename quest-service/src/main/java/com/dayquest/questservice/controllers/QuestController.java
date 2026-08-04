package com.dayquest.questservice.controllers;

import com.dayquest.questservice.dto.QuestDTO;
import com.dayquest.questservice.dto.CreateQuestDTO;
import com.dayquest.questservice.models.Quest;
import com.dayquest.questservice.repositories.QuestRepository;
import com.dayquest.questservice.services.QuestService;
import com.dayquest.questservice.services.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/quests")
@Tag(name = "Quests", description = "Quest creation, discovery, rating, and the daily quest system")
public class QuestController {

    private static final Logger logger = LoggerFactory.getLogger(QuestController.class);

    @Autowired
    private QuestService questService;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private RatingService ratingService;

    /**
     * Get paginated list of quests with sorting (like monolith)
     */
    @GetMapping
    @Async
    @Cacheable(value = "quests", key = "#page + ':' + #size + ':' + #sortBy + ':' + #sortDirection")
    @Operation(summary = "Get quests (paginated)", description = "Returns a paginated and sortable list of active quests. Includes like/dislike status for the requesting user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of quests returned"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<QuestDTO>>> getQuests(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field: createdAt, score, likes, title") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: ASC or DESC") @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate sortBy field
                List<String> allowedSortFields = List.of("createdAt", "score", "likes", "title");
                final String effectiveSortBy = allowedSortFields.contains(sortBy) ? sortBy : "createdAt";

                Sort sort = Sort.by(sortDirection, effectiveSortBy);
                PageRequest pageRequest = PageRequest.of(page, Math.min(size, 50), sort);

                Page<Quest> questPage = questRepository.findActiveQuestsByDate(pageRequest);

                // Get user's liked and disliked quests
                Set<UUID> likedQuests = ratingService.getLikedQuestIds(userId);
                Set<UUID> dislikedQuests = ratingService.getDislikedQuestIds(userId);

                List<QuestDTO> questDTOs = questPage.getContent().stream()
                        .map(quest -> toQuestDTO(quest, likedQuests, dislikedQuests))
                        .collect(Collectors.toList());

                return ResponseEntity.ok(questDTOs);
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Error fetching quests", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        });
    }

    /**
     * Get a single quest by ID
     */
    @GetMapping("/{uuid}")
    @Async
    @Cacheable(value = "quest", key = "#uuid", unless = "#result == null")
    @Operation(summary = "Get quest by UUID", description = "Returns a single quest by its UUID, including like/dislike status for the requesting user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest returned"),
            @ApiResponse(responseCode = "404", description = "Quest not found")
    })
    public CompletableFuture<ResponseEntity<QuestDTO>> getQuest(
            @Parameter(description = "UUID of the quest", required = true) @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") UUID userId) {

        return CompletableFuture.supplyAsync(() -> {
            Optional<Quest> questOpt = questRepository.findByUuidAndDeletedFalse(uuid);
            if (questOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Set<UUID> likedQuests = ratingService.getLikedQuestIds(userId);
            Set<UUID> dislikedQuests = ratingService.getDislikedQuestIds(userId);

            return ResponseEntity.ok(toQuestDTO(questOpt.get(), likedQuests, dislikedQuests));
        });
    }


    @PostMapping
    @Async
    @CacheEvict(value = {"quests", "topQuests"}, allEntries = true)
    @Operation(summary = "Create a new quest", description = "Creates a new quest. The creator UUID and username are extracted from the gateway-injected headers.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quest created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public CompletableFuture<ResponseEntity<QuestDTO>> createQuest(
            @Valid @RequestBody CreateQuestDTO createQuestDTO,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Name", required = false) String username) {

        return CompletableFuture.supplyAsync(() -> {
            Quest quest = questService.createQuest(
                    createQuestDTO.getTitle(),
                    createQuestDTO.getDescription(),
                    userId,
                    username
            ).join();

            if (quest == null) {
                return ResponseEntity.badRequest().build();
            }

            logger.info("Quest created: {} by user {}", quest.getUuid(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(toQuestDTO(quest, Set.of(), Set.of()));
        });
    }

    /**
     * Delete a quest (soft delete)
     */
    @DeleteMapping("/{uuid}")
    @Async
    @CacheEvict(value = {"quests", "quest", "topQuests"}, allEntries = true)
    @Operation(summary = "Delete a quest", description = "Soft-deletes a quest. Only the creator can delete their own quest.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest deleted"),
            @ApiResponse(responseCode = "403", description = "Not the quest creator"),
            @ApiResponse(responseCode = "404", description = "Quest not found")
    })
    public CompletableFuture<ResponseEntity<String>> deleteQuest(
            @Parameter(description = "UUID of the quest to delete", required = true) @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") UUID userId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean deleted = questService.deleteQuest(uuid, userId);
                if (!deleted) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok("Quest deleted successfully");
            } catch (RuntimeException e) {
                if (e.getMessage().equals("Only creator can delete quest")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
                }
                throw e;
            }
        });
    }


    /**
     * Also support path-based like/dislike for REST-style API
     */
    @PostMapping("/{uuid}/like")
    @CacheEvict(value = {"quests", "quest", "userRatings"}, allEntries = true)
    @Operation(summary = "Like a quest", description = "Likes the quest with the given UUID on behalf of the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest liked"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "409", description = "Already rated")
    })
    public CompletableFuture<ResponseEntity<String>> likeQuestPath(
            @Parameter(description = "UUID of the quest", required = true) @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") UUID userId) {

        return ratingService.rateQuest(userId, uuid, true);
    }

    @DeleteMapping("/{uuid}/like")
    @CacheEvict(value = {"quests", "quest", "userRatings"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> unlikeQuestPath(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") UUID userId) {

        return ratingService.removeRating(userId, uuid);
    }

    @PostMapping("/{uuid}/dislike")
    @CacheEvict(value = {"quests", "quest", "userRatings"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> dislikeQuestPath(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") UUID userId) {

        return ratingService.rateQuest(userId, uuid, false);
    }

    @DeleteMapping("/{uuid}/dislike")
    @CacheEvict(value = {"quests", "quest", "userRatings"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> undislikeQuestPath(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") UUID userId) {

        return ratingService.removeRating(userId, uuid);
    }


    @PostMapping("/{uuid}/rate")
    @CacheEvict(value = {"quests", "quest", "userRatings", "userLikedQuests", "userDislikedQuests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> rateQuest(
            @PathVariable UUID uuid,
            @RequestParam boolean isLike,
            @RequestHeader("X-User-Id") UUID userId) {

        return ratingService.rateQuest(userId, uuid, isLike);
    }


    @GetMapping("/user/{userId}")
    @Async
    @Cacheable(value = "userQuests", key = "#userId + ':' + #page + ':' + #size")
    public CompletableFuture<ResponseEntity<List<QuestDTO>>> getQuestsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return CompletableFuture.supplyAsync(() -> {
            PageRequest pageRequest = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Quest> questPage = questRepository.findByCreatorUuidAndDeletedFalse(userId, pageRequest);

            List<QuestDTO> questDTOs = questPage.getContent().stream()
                    .map(quest -> toQuestDTO(quest, Set.of(), Set.of()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(questDTOs);
        });
    }


    @GetMapping("/top")
    @Async
    @Cacheable(value = "topQuests", unless = "#result == null")
    public CompletableFuture<ResponseEntity<List<QuestDTO>>> getTopQuests(
            @RequestParam(defaultValue = "10") int limit) {

        return CompletableFuture.supplyAsync(() -> {
            PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 50));
            Page<Quest> quests = questRepository.findTopQuestsByScore(pageRequest);

            List<QuestDTO> questDTOs = quests.getContent().stream()
                    .map(quest -> toQuestDTO(quest, Set.of(), Set.of()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(questDTOs);
        });
    }

    /**
     * Get a random quest for daily quest
     */
    @GetMapping("/random")
    @Async
    public CompletableFuture<ResponseEntity<QuestDTO>> getRandomQuest() {

        return questService.getRandomQuest().thenApply(quest -> {
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(toQuestDTO(quest, Set.of(), Set.of()));
        });
    }

    /**
     * Get daily quest for current user (assigned or new)
     */
    @GetMapping("/daily")
    @Async
    @Operation(summary = "Get daily quest", description = "Returns the daily quest assigned to the authenticated user. Assigns a new random quest if none exists for today.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daily quest returned"),
            @ApiResponse(responseCode = "404", description = "No quest available")
    })
    public CompletableFuture<ResponseEntity<QuestDTO>> getDailyQuest(
            @RequestHeader("X-User-Id") UUID userId) {

        return questService.getDailyQuestForUser(userId).thenApply(quest -> {
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(toQuestDTO(quest, Set.of(), Set.of()));
        });
    }

    /**
     * Reroll daily quest
     */
    @PostMapping("/daily/reroll")
    @Async
    @Operation(summary = "Reroll daily quest", description = "Replaces the current daily quest with a new random one. Limited to 3 rerolls per day.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New daily quest returned"),
            @ApiResponse(responseCode = "429", description = "No rerolls remaining today")
    })
    public CompletableFuture<ResponseEntity<?>> rerollDailyQuest(
            @RequestHeader("X-User-Id") UUID userId) {

        return questService.rerollDailyQuest(userId).thenApply(result -> {
            if (result == null) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "No rerolls remaining today"));
            }
            return ResponseEntity.ok(toQuestDTO(result, Set.of(), Set.of()));
        });
    }

    @GetMapping("/daily/rerolls")
    @Async
    @Operation(summary = "Get remaining rerolls", description = "Returns the number of daily quest rerolls used and remaining for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reroll status returned")
    })
    public CompletableFuture<ResponseEntity<?>> getRemainingRerolls(
            @RequestHeader("X-User-Id") UUID userId) {

        return questService.getRemainingRerolls(userId)
                .thenApply(ResponseEntity::ok);
    }

    private QuestDTO toQuestDTO(Quest quest, Set<UUID> likedQuests, Set<UUID> dislikedQuests) {
        QuestDTO dto = new QuestDTO();
        dto.setUuid(quest.getUuid());
        dto.setCreatorUuid(quest.getCreatorUuid());
        dto.setCreatorUsername(quest.getCreatorUsername());
        dto.setTitle(quest.getTitle());
        dto.setDescription(quest.getDescription());
        dto.setLikes(quest.getLikes());
        dto.setDislikes(quest.getDislikes());
        dto.setScore(quest.getScore());
        dto.setVideoCount(quest.getVideoCount());
        dto.setCreatedAt(quest.getCreatedAt());
        dto.setLiked(likedQuests.contains(quest.getUuid()));
        dto.setDisliked(dislikedQuests.contains(quest.getUuid()));
        return dto;
    }
}
