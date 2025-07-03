package com.dayquest.dayquestbackend.user.services;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.hashtag.HashtagService;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.user.ids.QuestRatingId;
import com.dayquest.dayquestbackend.user.ids.VideoRatingId;
import com.dayquest.dayquestbackend.user.models.QuestRating;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.models.VideoRating;
import com.dayquest.dayquestbackend.user.repositories.QuestRatingRepository;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import com.dayquest.dayquestbackend.user.repositories.VideoRatingRepository;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final QuestRatingRepository questRatingRepository;
    private final VideoRatingRepository videoRatingRepository;
    private final VideoRepository videoRepository;
    private final HashtagService hashtagService;

    /* ================================= QUESTS ================================= */

    @Async
    @CacheEvict(value = {"quests", "userProfiles"}, allEntries = true) // Consider more specific eviction
    public CompletableFuture<ResponseEntity<String>> rateQuest(String bearerToken, UUID questId, boolean like) {
        return CompletableFuture.supplyAsync(() -> {
            User user = resolveUser(bearerToken);
            if (user != null) {
                evictUserSpecificCaches(user.getUuid());
            }

            return doRate(
                    bearerToken,
                    questId,
                    like,
                    questRepository::findById,
                    (userId, targetId) -> new QuestRatingId(userId, targetId),
                    questRatingRepository::findById,
                    (userEntity, quest) -> new QuestRating(userEntity, quest, like),
                    questRatingRepository::save,
                    (oldLike, newLike) -> updateQuestCounters(questId, oldLike, newLike)
            );
        });
    }

    @Async
    @CacheEvict(value = {"quests", "userProfiles"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> removeQuestRating(String bearerToken, UUID questId) {
        return CompletableFuture.completedFuture(
                doRemoveRating(
                        bearerToken,
                        questId,
                        questRepository::findById,
                        (userId, targetId) -> new QuestRatingId(userId, targetId),
                        questRatingRepository::findById,
                        questRatingRepository::delete,
                        wasLike -> updateQuestCountersAfterDelete(questId, wasLike)
                )
        );
    }

    /* ================================= VIDEOS ================================= */

    @Async
    public CompletableFuture<ResponseEntity<String>> rateVideo(String bearerToken, UUID videoId, boolean like) {
        return CompletableFuture.completedFuture(
                doRate(
                        bearerToken,
                        videoId,
                        like,
                        videoRepository::findById,
                        (userId, targetId) -> new VideoRatingId(userId, targetId),
                        videoRatingRepository::findById,
                        (user, video) -> new VideoRating(user, video, like),
                        videoRatingRepository::save,
                        (oldLike, newLike) -> updateVideoCounters(videoId, oldLike, newLike, extractUserFromToken(bearerToken))
                )
        );
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> removeVideoRating(String bearerToken, UUID videoId) {
        return CompletableFuture.completedFuture(
                doRemoveRating(
                        bearerToken,
                        videoId,
                        videoRepository::findById,
                        (userId, targetId) -> new VideoRatingId(userId, targetId),
                        videoRatingRepository::findById,
                        videoRatingRepository::delete,
                        wasLike -> updateVideoCountersAfterDelete(videoId, wasLike, extractUserFromToken(bearerToken))
                )
        );
    }

    /* ============================= Public Helper Methods ============================= */

    @Cacheable(value = "userLikedQuests", key = "#user.uuid", unless = "#result.isEmpty()")
    public List<UUID> getLikedQuests(User user) {
        return findQuestIds(user, true);
    }

    @Cacheable(value = "userDislikedQuests", key = "#user.uuid", unless = "#result.isEmpty()")
    public List<UUID> getDislikedQuests(User user) {
        return findQuestIds(user, false);
    }

    @CacheEvict(value = {"userLikedQuests", "userDislikedQuests", "userLikedVideos", "userDislikedVideos"}, key = "#userId")
    public void evictUserSpecificCaches(UUID userId) {
    }

    @Cacheable(value = "userProfiles", key = "#user.uuid + '_liked_videos'")
    public List<UUID> getLikedVideos(User user) {
        return findVideoIds(user, true);
    }

    @Cacheable(value = "userProfiles", key = "#user.uuid + '_disliked_videos'")
    public List<UUID> getDislikedVideos(User user) {
        return findVideoIds(user, false);
    }

    /* ============================= Private Helper Methods ============================= */

    private <E, R, ID> ResponseEntity<String> doRate(
            String bearerToken,
            UUID targetId,
            boolean newLike,
            java.util.function.Function<UUID, java.util.Optional<E>> entityFinder,
            BiFunction<UUID, UUID, ID> idFactory,
            java.util.function.Function<ID, java.util.Optional<R>> ratingLoader,
            BiFunction<User, E, R> ratingFactory,
            java.util.function.Function<R, R> ratingSaver,
            BiConsumer<Boolean, Boolean> counterUpdater) {

        User user = resolveUser(bearerToken);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        E target = entityFinder.apply(targetId).orElse(null);
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Target not found");
        }

        ID ratingId = idFactory.apply(user.getUuid(), targetId);
        R existingRating = ratingLoader.apply(ratingId).orElse(null);

        Boolean oldLike = null;
        if (existingRating != null) {
            oldLike = extractLikeValue(existingRating);
            if (oldLike != null && oldLike == newLike) {
                return ResponseEntity.badRequest().body("Already rated the same way");
            }
        }

        counterUpdater.accept(oldLike, newLike);

        if (existingRating == null) {
            ratingSaver.apply(ratingFactory.apply(user, target));
            return ResponseEntity.ok("Rating added");
        }

        // Update existing rating
        updateRatingLikeValue(existingRating, newLike);
        ratingSaver.apply(existingRating);
        return ResponseEntity.ok("Rating updated");
    }

    private <E, R, ID> ResponseEntity<String> doRemoveRating(
            String bearerToken,
            UUID targetId,
            java.util.function.Function<UUID, java.util.Optional<E>> entityFinder,
            BiFunction<UUID, UUID, ID> idFactory,
            java.util.function.Function<ID, java.util.Optional<R>> ratingLoader,
            java.util.function.Consumer<R> ratingDeleter,
            Consumer<Boolean> afterDeleteCounter) {

        User user = resolveUser(bearerToken);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        if (entityFinder.apply(targetId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Target not found");
        }

        ID ratingId = idFactory.apply(user.getUuid(), targetId);
        R rating = ratingLoader.apply(ratingId).orElse(null);
        if (rating == null) {
            return ResponseEntity.badRequest().body("No rating to remove");
        }

        Boolean wasLike = extractLikeValue(rating);
        if (wasLike == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Invalid rating state");
        }

        ratingDeleter.accept(rating);
        afterDeleteCounter.accept(wasLike);
        return ResponseEntity.ok("Rating removed");
    }

    /* ------------------------ Counter Updates for Quests/Videos ------------------------ */

    private void updateQuestCounters(UUID questId, Boolean oldLike, Boolean newLike) {
        if (oldLike != null) {
            if (oldLike) {
                questRepository.decrementLikes(questId);
            } else {
                questRepository.decrementDislikes(questId);
            }
        }

        if (newLike) {
            questRepository.incrementLikes(questId);
        } else {
            questRepository.incrementDislikes(questId);
        }
    }

    private void updateQuestCountersAfterDelete(UUID questId, boolean wasLike) {
        if (wasLike) {
            questRepository.decrementLikes(questId);
        } else {
            questRepository.decrementDislikes(questId);
        }
    }

    private void updateVideoCounters(UUID videoId, Boolean oldLike, Boolean newLike, User user) {
        if (oldLike != null) {
            if (oldLike) {
                videoRepository.decrementUpVotes(videoId);
            } else {
                videoRepository.decrementDownVotes(videoId);
            }
        }

        if (newLike) {
            videoRepository.incrementUpVotes(videoId);
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video != null && user != null) {
                hashtagService.likeHashtags(video.getHashtags(), user);
            }
        } else {
            videoRepository.incrementDownVotes(videoId);
        }
    }

    private void updateVideoCountersAfterDelete(UUID videoId, boolean wasLike, User user) {
        if (wasLike) {
            videoRepository.decrementUpVotes(videoId);
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video != null && user != null) {
                //TODO: Also unlike the hashtag
            }
        } else {
            videoRepository.decrementDownVotes(videoId);
        }
    }

    /* ------------------------------ Utility Methods ------------------------------ */

    private User resolveUser(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            UUID userId = jwtService.extractUserId(bearerToken.substring(BEARER_PREFIX.length()));
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            log.error("Error extracting user from token", e);
            return null;
        }
    }

    private User extractUserFromToken(String bearerToken) {
        return resolveUser(bearerToken);
    }

    private Boolean extractLikeValue(Object rating) {
        if (rating instanceof QuestRating qr) {
            return qr.isLiked();
        } else if (rating instanceof VideoRating vr) {
            return vr.isLiked();
        }
        return null;
    }

    private void updateRatingLikeValue(Object rating, boolean newLike) {
        if (rating instanceof QuestRating qr) {
            qr.setLiked(newLike);
        } else if (rating instanceof VideoRating vr) {
            vr.setLiked(newLike);
        }
    }

    private List<UUID> findQuestIds(User user, boolean liked) {
        if (user == null) return List.of();
        return (liked ? questRatingRepository.findByUserAndLikedTrue(user)
                : questRatingRepository.findByUserAndLikedFalse(user))
                .stream()
                .map(qr -> qr.getQuest().getUuid())
                .toList();
    }

    private List<UUID> findVideoIds(User user, boolean liked) {
        if (user == null) return List.of();
        return (liked ? videoRatingRepository.findByUserAndLikedTrue(user)
                : videoRatingRepository.findByUserAndLikedFalse(user))
                .stream()
                .map(vr -> vr.getVideo().getUuid())
                .toList();
    }
}