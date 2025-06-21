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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class RatingService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final QuestRatingRepository questRatingRepository;
    private final VideoRatingRepository videoRatingRepository;
    private final VideoRepository videoRepository;
    private final HashtagService hashtagService;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public RatingService(JwtService jwtService,
                         UserRepository userRepository,
                         QuestRepository questRepository,
                         QuestRatingRepository questRatingRepository,
                         VideoRatingRepository videoRatingRepository,
                         VideoRepository videoRepository,
                         HashtagService hashtagService,
                         TransactionTemplate transactionTemplate) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.questRepository = questRepository;
        this.questRatingRepository = questRatingRepository;
        this.videoRatingRepository = videoRatingRepository;
        this.videoRepository = videoRepository;
        this.hashtagService = hashtagService;
        this.transactionTemplate = transactionTemplate;
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> rateQuestAsync(String token, UUID questId, boolean isLike) {
        return CompletableFuture.supplyAsync(() -> {
            return transactionTemplate.execute(status -> {
                return executeRateQuest(token, questId, isLike);
            });
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> removeQuestRatingAsync(String token, UUID questId) {
        return CompletableFuture.supplyAsync(() -> {
            return transactionTemplate.execute(status -> {
                return executeRemoveQuestRating(token, questId);
            });
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> rateVideoAsync(String token, UUID videoId, boolean isLike) {
        return CompletableFuture.supplyAsync(() -> {
            return transactionTemplate.execute(status -> {
                return executeRateVideo(token, videoId, isLike);
            });
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> removeVideoRatingAsync(String token, UUID videoId) {
        return CompletableFuture.supplyAsync(() -> {
            return transactionTemplate.execute(status -> {
                return executeRemoveVideoRating(token, videoId);
            });
        });
    }

    @Transactional
    public ResponseEntity<String> rateQuest(String token, UUID questId, boolean isLike) {
        return executeRateQuest(token, questId, isLike);
    }

    @Transactional
    public ResponseEntity<String> removeQuestRating(String token, UUID questId) {
        return executeRemoveQuestRating(token, questId);
    }

    @Transactional
    public ResponseEntity<String> rateVideo(String token, UUID videoId, boolean isLike) {
        return executeRateVideo(token, videoId, isLike);
    }

    @Transactional
    public ResponseEntity<String> removeVideoRating(String token, UUID videoId) {
        return executeRemoveVideoRating(token, videoId);
    }

    private ResponseEntity<String> executeRateQuest(String token, UUID questId, boolean isLike) {
        UUID userId = jwtService.extractUserId(token.substring(7));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid user");
        }

        Quest quest = questRepository.findById(questId).orElse(null);
        if (quest == null) {
            return ResponseEntity.notFound().build();
        }

        QuestRatingId ratingId = new QuestRatingId(userId, questId);
        Optional<QuestRating> existingRating = questRatingRepository.findById(ratingId);

        if (existingRating.isPresent()) {
            QuestRating rating = existingRating.get();
            if (rating.isLiked() == isLike) {
                return ResponseEntity.badRequest().body("Already rated this quest");
            }

            rating.setLiked(isLike);
            questRatingRepository.save(rating);

            if (isLike) {
                questRepository.incrementLikes(questId);
                questRepository.decrementDislikes(questId);
            } else {
                questRepository.decrementLikes(questId);
                questRepository.incrementDislikes(questId);
            }

            return ResponseEntity.ok("Rating updated successfully");
        }

        QuestRating questRating = new QuestRating(user, quest, isLike);
        questRatingRepository.save(questRating);

        if (isLike) {
            questRepository.incrementLikes(questId);
        } else {
            questRepository.incrementDislikes(questId);
        }

        return ResponseEntity.ok("Rating added successfully");
    }

    private ResponseEntity<String> executeRemoveQuestRating(String token, UUID questId) {
        UUID userId = jwtService.extractUserId(token.substring(7));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid user");
        }

        Quest quest = questRepository.findById(questId).orElse(null);
        if (quest == null) {
            return ResponseEntity.notFound().build();
        }

        QuestRatingId ratingId = new QuestRatingId(userId, questId);
        Optional<QuestRating> existingRating = questRatingRepository.findById(ratingId);

        if (existingRating.isPresent()) {
            QuestRating rating = existingRating.get();
            questRatingRepository.delete(rating);

            if (rating.isLiked()) {
                questRepository.decrementLikes(questId);
            } else {
                questRepository.decrementDislikes(questId);
            }

            return ResponseEntity.ok("Rating removed successfully");
        }

        return ResponseEntity.badRequest().body("No rating found for this quest");
    }

    private ResponseEntity<String> executeRateVideo(String token, UUID videoId, boolean isLike) {
        UUID userId = jwtService.extractUserId(token.substring(7));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid user");
        }

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return ResponseEntity.notFound().build();
        }

        VideoRatingId ratingId = new VideoRatingId(userId, videoId);
        Optional<VideoRating> existingRating = videoRatingRepository.findById(ratingId);

        if (existingRating.isPresent()) {
            VideoRating rating = existingRating.get();
            if (rating.isLiked() == isLike) {
                return ResponseEntity.badRequest().body("Already rated this video");
            }

            rating.setLiked(isLike);
            videoRatingRepository.save(rating);

            if (isLike) {
                videoRepository.incrementUpVotes(videoId);
                videoRepository.decrementDownVotes(videoId);
                hashtagService.likeHashtags(video.getHashtags(), user);
            } else {
                videoRepository.decrementUpVotes(videoId);
                videoRepository.incrementDownVotes(videoId);
            }

            return ResponseEntity.ok("Rating updated successfully");
        }

        VideoRating videoRating = new VideoRating(user, video, isLike);
        videoRatingRepository.save(videoRating);

        if (isLike) {
            videoRepository.incrementUpVotes(videoId);
            hashtagService.likeHashtags(video.getHashtags(), user);
        } else {
            videoRepository.incrementDownVotes(videoId);
        }

        return ResponseEntity.ok("Rating added successfully");
    }

    private ResponseEntity<String> executeRemoveVideoRating(String token, UUID videoId) {
        UUID userId = jwtService.extractUserId(token.substring(7));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid user");
        }

        VideoRatingId ratingId = new VideoRatingId(userId, videoId);
        Optional<VideoRating> existingRating = videoRatingRepository.findById(ratingId);

        if (existingRating.isPresent()) {
            VideoRating rating = existingRating.get();
            videoRatingRepository.delete(rating);

            if (rating.isLiked()) {
                videoRepository.decrementUpVotes(videoId);
            } else {
                videoRepository.decrementDownVotes(videoId);
            }

            return ResponseEntity.ok("Rating removed successfully");
        }

        return ResponseEntity.badRequest().body("No rating found for this video");
    }

    @Transactional(readOnly = true)
    public List<UUID> getLikedQuests(User user) {
        if (user == null) {
            return List.of();
        }
        return questRatingRepository.findByUserAndLikedTrue(user).stream()
                .map(QuestRating::getQuest)
                .map(Quest::getUuid)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> getDislikedQuests(User user) {
        if (user == null) {
            return List.of();
        }
        return questRatingRepository.findByUserAndLikedFalse(user).stream()
                .map(QuestRating::getQuest)
                .map(Quest::getUuid)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> getLikedVideos(User user) {
        if (user == null) {
            return List.of();
        }
        return videoRatingRepository.findByUserAndLikedTrue(user).stream()
                .map(VideoRating::getVideo)
                .map(Video::getUuid)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> getDislikedVideos(User user) {
        if (user == null) {
            return List.of();
        }
        return videoRatingRepository.findByUserAndLikedFalse(user).stream()
                .map(VideoRating::getVideo)
                .map(Video::getUuid)
                .toList();
    }
}