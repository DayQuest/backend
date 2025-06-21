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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class RatingService {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestRepository questRepository;
    @Autowired
    private QuestRatingRepository questRatingRepository;
    @Autowired
    private VideoRatingRepository videoRatingRepository;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private HashtagService hashtagService;

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<String>> rateQuest(String token, UUID questId, boolean isLike) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            User user = userRepository.findById(userId).orElse(null);
            Quest quest = questRepository.findById(questId).orElse(null);
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }

            QuestRatingId ratingId = new QuestRatingId(user.getUuid(), quest.getUuid());
            Optional<QuestRating> questRatingOpt = questRatingRepository.findById(ratingId);
            if (questRatingOpt.isPresent() && questRatingOpt.get().isLiked() == isLike) {
                return ResponseEntity.badRequest().body("Already rated this quest");
            }
            if (questRatingOpt.isPresent()) {
                QuestRating existingRating = questRatingOpt.get();
                existingRating.setLiked(isLike);
                questRatingRepository.save(existingRating);
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
        });
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<String>> removeQuestRating(String token, UUID questId) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            User user = userRepository.findById(userId).orElse(null);
            Quest quest = questRepository.findById(questId).orElse(null);
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }

            QuestRatingId ratingId = new QuestRatingId(user.getUuid(), quest.getUuid());
            Optional<QuestRating> questRatingOpt = questRatingRepository.findById(ratingId);
            if (questRatingOpt.isPresent()) {
                QuestRating existingRating = questRatingOpt.get();
                questRatingRepository.delete(existingRating);
                if (existingRating.isLiked()) {
                    questRepository.decrementLikes(questId);
                } else {
                    questRepository.decrementDislikes(questId);
                }
                return ResponseEntity.ok("Rating removed successfully");
            }
            return ResponseEntity.badRequest().body("No rating found for this quest");
        });
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<String>> rateVideo(String token, UUID videoId, boolean isLike) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            User user = userRepository.findById(userId).orElse(null);
            VideoRatingId ratingId = new VideoRatingId(user.getUuid(), videoId);
            Optional<VideoRating> videoRatingOpt = videoRatingRepository.findById(ratingId);
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null) {
                return ResponseEntity.notFound().build();
            }
            if (videoRatingOpt.isPresent() && videoRatingOpt.get().isLiked() == isLike) {
                return ResponseEntity.badRequest().body("Already rated this video");
            }
            if (videoRatingOpt.isPresent()) {
                VideoRating existingRating = videoRatingOpt.get();
                existingRating.setLiked(isLike);
                videoRatingRepository.save(existingRating);
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
        });
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<String>> removeVideoRating(String token, UUID videoId){
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            User user = userRepository.findById(userId).orElse(null);
            VideoRatingId ratingId = new VideoRatingId(user.getUuid(), videoId);
            Optional<VideoRating> videoRatingOpt = videoRatingRepository.findById(ratingId);
            if (videoRatingOpt.isPresent()) {
                VideoRating existingRating = videoRatingOpt.get();
                videoRatingRepository.delete(existingRating);
                if (existingRating.isLiked()) {
                    videoRepository.decrementUpVotes(videoId);
                } else {
                    videoRepository.decrementDownVotes(videoId);
                }
                return ResponseEntity.ok("Rating removed successfully");
            }
            return ResponseEntity.badRequest().body("No rating found for this video");
        });
    }

    public List<UUID> getLikedQuests(User user) {
        if (user == null) {
            return null;
        }
        return questRatingRepository.findByUserAndLikedTrue(user).stream().map(
                QuestRating::getQuest
        ).map(Quest::getUuid).toList();
    }

    public List<UUID> getDislikedQuests(User user) {
        if (user == null) {
            return null;
        }
        return questRatingRepository.findByUserAndLikedFalse(user).stream().map(
                QuestRating::getQuest
        ).map(Quest::getUuid).toList();

    }

    public List<UUID> getLikedVideos(User user) {
        if (user == null) {
            return null;
        }
        return videoRatingRepository.findByUserAndLikedTrue(user).stream().map(
                VideoRating::getVideo
        ).map(Video::getUuid).toList();

    }

    public List<UUID> getDislikedVideos(User user) {
        if (user == null) {
            return null;
        }
        return videoRatingRepository.findByUserAndLikedFalse(user).stream().map(
                VideoRating::getVideo
        ).map(Video::getUuid).toList();

    }
}
