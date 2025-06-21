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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final QuestRepository questRepository;
    private final QuestRatingRepository questRatingRepository;
    private final VideoRatingRepository videoRatingRepository;
    private final VideoRepository videoRepository;
    private final HashtagService hashtagService;

    @Async
    public CompletableFuture<ResponseEntity<String>> rateQuestAsync(String token, UUID questId, boolean isLike) {
        return CompletableFuture.completedFuture(rateQuest(token, questId, isLike));
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> removeQuestRatingAsync(String token, UUID questId) {
        return CompletableFuture.completedFuture(removeQuestRating(token, questId));
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> rateVideoAsync(String token, UUID videoId, boolean isLike) {
        return CompletableFuture.completedFuture(rateVideo(token, videoId, isLike));
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> removeVideoRatingAsync(String token, UUID videoId) {
        return CompletableFuture.completedFuture(removeVideoRating(token, videoId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<String> rateQuest(String token, UUID questId, boolean isLike) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<String> removeQuestRating(String token, UUID questId) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<String> rateVideo(String token, UUID videoId, boolean isLike) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<String> removeVideoRating(String token, UUID videoId) {
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
        if (user == null) return List.of();
        return questRatingRepository.findQuestIdsByUserAndLiked(user.getUuid(), true);
    }

    @Transactional(readOnly = true)
    public List<UUID> getDislikedQuests(User user) {
        if (user == null) return List.of();
        return questRatingRepository.findQuestIdsByUserAndLiked(user.getUuid(), false);
    }

    @Transactional(readOnly = true)
    public List<UUID> getLikedVideos(User user) {
        if (user == null) return List.of();
        return videoRatingRepository.findVideoIdsByUserAndLiked(user.getUuid(), true);
    }

    @Transactional(readOnly = true)
    public List<UUID> getDislikedVideos(User user) {
        if (user == null) return List.of();
        return videoRatingRepository.findVideoIdsByUserAndLiked(user.getUuid(), false);
    }
}