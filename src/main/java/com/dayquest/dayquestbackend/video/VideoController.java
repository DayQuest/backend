package com.dayquest.dayquestbackend.video;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.common.dto.UuidDTO;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.storage.service.ThumbnailStorageService;
import com.dayquest.dayquestbackend.user.models.User;

import com.dayquest.dayquestbackend.user.repositories.UserRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.user.services.FollowService;
import com.dayquest.dayquestbackend.user.services.RatingService;
import com.dayquest.dayquestbackend.video.dto.VideoDTO;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.models.ViewedVideo;
import com.dayquest.dayquestbackend.video.models.ViewedVideoId;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import com.dayquest.dayquestbackend.video.repository.ViewedVideoRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.*;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ViewedVideoRepository viewedVideoRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private AsyncTaskExecutor delegatingSecurityContextAsyncTaskExecutor;

    @Autowired
    private ActivityUpdater activityUpdater;
    @Autowired
    private ThumbnailStorageService thumbnailStorageService;

    private static final Logger logger = Logger.getLogger(VideoController.class.getName());
    @Autowired
    private FollowService followService;
    @Autowired
    private RatingService ratingService;

    @Async
    @PostMapping("/upload")
    @CacheEvict(value = "videos", allEntries = true)
    public CompletableFuture<ResponseEntity<String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestHeader("Authorization") String token,
            @RequestParam("hashtags") List<String> hashtags) {

        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            Optional<User> user = Optional.ofNullable(userRepository.findByUsername(username));
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            videoService.uploadVideo(file, title, description, user.get(), hashtags).join();
            activityUpdater.increaseInteractions(user);
            return ResponseEntity.ok("Uploaded");
        });
    }

    @Async
    @DeleteMapping("/{uuid}")
    @CacheEvict(value = "videos", allEntries = true)
    public CompletableFuture<ResponseEntity<String>> deleteVideo(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Video> video = videoRepository.findById(uuid);
            if (video.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String username = jwtService.extractUsername(token);
            if (!video.get().getUser().getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            activityUpdater.increaseInteractions(video.get().getUser());
            videoRepository.delete(video.get());
            return ResponseEntity.ok("Deleted");
        });
    }


    @Async
    @PostMapping("/next-vid")
    public CompletableFuture<ResponseEntity<?>> nextVideo(@RequestBody UUID userUuid) {
        return userRepository.findById(userUuid)
                .map(user -> CompletableFuture.supplyAsync(() -> {
                    List<Video> unviewedVideos = videoRepository.findUnviewedVideosByUserId(user.getUuid());
                    if (unviewedVideos.isEmpty()) {
                        Optional<Video> randomVideoOpt = videoRepository.findRandomVideo();
                        if (randomVideoOpt.isPresent()) {
                            Video randomVideo = randomVideoOpt.get();
                            viewedVideoRepository.save(new ViewedVideo(new ViewedVideoId(user.getUuid(), randomVideo.getUuid())));
                            return ResponseEntity.ok(createVideoDTO(randomVideo, user));
                        } else {
                            return ResponseEntity.noContent().build();
                        }
                    }

                    Video video = unviewedVideos.get(0);
                    viewedVideoRepository.save(new ViewedVideo(new ViewedVideoId(user.getUuid(), video.getUuid())));
                    video.setViews(video.getViews() + 1);
                    return ResponseEntity.ok(createVideoDTO(video, user));
                }))
                .orElseGet(() -> CompletableFuture.completedFuture(
                        ResponseEntity.notFound().build()
                ));
    }

    public VideoDTO createVideoDTO(Video video, User user) {
        VideoDTO videoDTO = new VideoDTO(
                video.getTitle(),
                video.getDescription(),
                video.getUpVotes(),
                video.getDownVotes(),
                video.getUser().getUsername(),
                video.getFilePath(),
                null,
                questRepository.findByUuid(video.getQuestUuid()),
                video.getUuid(),
                video.getCreatedAt(),
                followService.isFollowing(user.getUuid(), video.getUser().getUuid()).join()
        );

        videoDTO.setLiked(ratingService.getLikedVideos(user).contains(video.getUuid()));
        videoDTO.setDisliked(ratingService.getDislikedQuests(user).contains(video.getUuid()));
        return videoDTO;
    }


    @PostMapping("/{uuid}/like")
    @Async
    @CacheEvict(value = {"videos", "userLikedVideos"}, allEntries = true)
    public CompletableFuture<Object> likeVideo(
            @PathVariable UUID uuid,
            @RequestBody UuidDTO userUuid,
            @RequestHeader("Authorization") String token) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> user = userRepository.findById(userUuid.getUuid());

                if (user.isEmpty() || !Objects.equals(user.get().getUsername(), jwtService.extractUsername(token.substring(7)))) {
                    return ResponseEntity.notFound().build();
                }

                activityUpdater.increaseInteractions(user);
                userRepository.save(user.get());
                return ratingService.rateVideo(token, uuid, true);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    @DeleteMapping("/{uuid}/like")
    @Async
    @CacheEvict(value = {"videos", "userLikedVideos"}, allEntries = true)
    public CompletableFuture<Object> unlikeVideo(
            @PathVariable UUID uuid,
            @RequestBody UuidDTO userUuid,
            @RequestHeader("Authorization") String token) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> user = userRepository.findById(userUuid.getUuid());

                if (user.isEmpty() || !Objects.equals(user.get().getUsername(), jwtService.extractUsername(token.substring(7)))) {
                    return ResponseEntity.notFound().build();
                }
                activityUpdater.increaseInteractions(user);
                userRepository.save(user.get());
                return ratingService.removeVideoRating(token, uuid);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return ResponseEntity.internalServerError().build();
            }
        });
    }


    @Async
    @PostMapping("/{uuid}/dislike")
    @CacheEvict(value = {"videos", "userDislikedVideos"}, allEntries = true)
    public CompletableFuture<Object> dislikeVideo(@PathVariable UUID uuid,
                                                  @RequestBody UuidDTO userUuid,
                                                  @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid.getUuid());

            if (user.isEmpty() || !Objects.equals(user.get().getUsername(), jwtService.extractUsername(token))) {
                return ResponseEntity.notFound().build();
            }

            activityUpdater.increaseInteractions(user);
            userRepository.save(user.get());
            return ratingService.rateVideo(token, uuid, false);
        });
    }

    @DeleteMapping("/{uuid}/dislike")
    @Async
    @CacheEvict(value = {"videos", "userDislikedVideos"}, allEntries = true)
    public CompletableFuture<Object> undislikeVideo(
            @PathVariable UUID uuid,
            @RequestBody UuidDTO userUuid,
            @RequestHeader("Authorization") String token) {

        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid.getUuid());
            if (user.isEmpty() || !Objects.equals(user.get().getUsername(), jwtService.extractUsername(token))) {
                return ResponseEntity.notFound().build();
            }
            activityUpdater.increaseInteractions(user);
            userRepository.save(user.get());
            return ratingService.removeVideoRating(token, uuid);
        });
    }


    @Async
    @GetMapping("/{uuid}")
    @Cacheable(value = "videos", key = "#uuid")
    public CompletableFuture<ResponseEntity<VideoDTO>> getVideoById(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Video> videoOpt = videoRepository.findById(uuid);
                if (videoOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }

                Video video = videoOpt.get();

                if (video.getUser() == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null);
                }

                return ResponseEntity.ok(createVideoDTO(video, video.getUser()));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error getting video: " + uuid, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        });
    }

    @GetMapping("/thumbnail/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<byte[]>> getDecodedImage(@PathVariable("uuid") String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Video> video = videoRepository.findById(UUID.fromString(uuid));
            return video.map(value -> ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnailStorageService.getThumbnail(uuid))).orElseGet(() -> ResponseEntity.notFound().build());

        });
    }
}
