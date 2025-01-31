package com.dayquest.dayquestbackend.video;

import com.dayquest.dayquestbackend.JwtService;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.user.ActivityUpdater;
import com.dayquest.dayquestbackend.user.User;

import com.dayquest.dayquestbackend.user.UserRepository;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.*;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.List;
import java.util.UUID;

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
    private Cache<Integer, String> videoCache;
    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private AsyncTaskExecutor delegatingSecurityContextAsyncTaskExecutor;

    @Autowired
    private ActivityUpdater activityUpdater;

    //new endpoint
    @Async
    @PostMapping
    public CompletableFuture<ResponseEntity<String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("userUuid") UUID userUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not find user with that UUID");
            }
            videoService.uploadVideo(file, title, description, user.get()).join();
            activityUpdater.increaseInteractions(user);
            return ResponseEntity.ok("Uploaded");
        });
    }


    //old endpoint just for the period where the frontend is not updated
    @Async
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<String>> uploadVideo1(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("userUuid") UUID userUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not find user with that UUID");
            }
            videoService.uploadVideo(file, title, description, user.get()).join();
            activityUpdater.increaseInteractions(user);
            return ResponseEntity.ok("Uploaded");
        });
    }

    @Async
    @DeleteMapping("/{uuid}")
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
                            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No videos available");
                        }
                    }

                    Video video = unviewedVideos.get(0);
                    viewedVideoRepository.save(new ViewedVideo(new ViewedVideoId(user.getUuid(), video.getUuid())));
                    video.setViews(video.getViews() + 1);
                    return ResponseEntity.ok(createVideoDTO(video, user));
                }))
                .orElseGet(() -> CompletableFuture.completedFuture(
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
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
                questRepository.findById(video.getQuestUuid()).orElse(null),
                video.getUuid(),
                video.getCreatedAt(),
                user.getFollowedUsers().contains(video.getUser().getUuid())
        );

        videoDTO.setLiked(user.getLikedVideos().contains(video.getUuid()));
        videoDTO.setDisliked(user.getDislikedVideos().contains(video.getUuid()));
        return videoDTO;
    }


    @PostMapping("/{uuid}/like")
    @Async
    public CompletableFuture<ResponseEntity<Video>> likeVideo(
            @PathVariable UUID uuid,
            @RequestBody UuidDTO userUuid) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> user = userRepository.findById(UUID.fromString(userUuid.getUuid()));
                Optional<Video> video = videoRepository.findById(uuid);

                if (user.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                if (user.get().getLikedVideos().contains(uuid)) {
                    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
                }

                if (user.get().getDislikedVideos().contains(uuid)) {
                    user.get().getDislikedVideos().remove(uuid);
                    video.get().setDownVotes(video.get().getDownVotes() - 1);
                    videoRepository.save(video.get());
                }

                user.get().getLikedVideos().add(uuid);

                activityUpdater.increaseInteractions(user);
                userRepository.save(user.get());
                return videoService.likeVideo(uuid).join();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, delegatingSecurityContextAsyncTaskExecutor);
    }

    @DeleteMapping("/{uuid}/like")
    @Async
    public CompletableFuture<ResponseEntity<Video>> unlikeVideo(
            @PathVariable UUID uuid,
            @RequestBody UuidDTO userUuid) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> user = userRepository.findById(UUID.fromString(userUuid.getUuid()));
                Optional<Video> video = videoRepository.findById(uuid);

                if (user.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                if (!user.get().getLikedVideos().contains(uuid)) {
                    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
                }

                user.get().getLikedVideos().remove(uuid);
                video.get().setUpVotes(video.get().getUpVotes() - 1);
                videoRepository.save(video.get());
                activityUpdater.increaseInteractions(user);
                userRepository.save(user.get());
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, delegatingSecurityContextAsyncTaskExecutor);
    }


    @Async
    @PostMapping("/{uuid}/dislike")
    public CompletableFuture<ResponseEntity<Video>> dislikeVideo(@PathVariable UUID uuid,
                                                                 @RequestBody UUID userUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid);
            Optional<Video> video = videoRepository.findById(uuid);
            if (user.isEmpty() || video.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (user.get().getDislikedVideos().contains(uuid)) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }

            if (user.get().getLikedVideos().contains(uuid)) {
                user.get().getLikedVideos().remove(uuid);
                video.get().setUpVotes(video.get().getUpVotes() - 1);
                videoRepository.save(video.get());

            }

            user.get().getDislikedVideos().add(uuid);
            userRepository.save(user.get());
            activityUpdater.increaseInteractions(user);
            return videoService.dislikeVideo(uuid).join();
        });
    }

    @DeleteMapping("/{uuid}/dislike")
    @Async
    public CompletableFuture<ResponseEntity<Video>> undislikeVideo(
            @PathVariable UUID uuid,
            @RequestBody UuidDTO userUuid) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<User> user = userRepository.findById(UUID.fromString(userUuid.getUuid()));
                Optional<Video> video = videoRepository.findById(uuid);

                if (user.isEmpty()) {
                }

                if (!user.get().getDislikedVideos().contains(uuid)) {
                    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
                }

                user.get().getDislikedVideos().remove(uuid);
                video.get().setDownVotes(video.get().getDownVotes() - 1);
                videoRepository.save(video.get());
                userRepository.save(user.get());
                activityUpdater.increaseInteractions(user);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, delegatingSecurityContextAsyncTaskExecutor);
    }

    @Async
    @GetMapping("/{uuid}")
    public CompletableFuture<ResponseEntity<VideoDTO>> getVideoById(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (videoRepository.findById(uuid).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Video video = videoRepository.findById(uuid).get();
            return ResponseEntity.ok(createVideoDTO(video, video.getUser()));
        });
    }

    @GetMapping("/thumbnail/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<ByteArrayResource>> getDecodedImage(@PathVariable("uuid") String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Video> videoOptional = videoRepository.findById(UUID.fromString(uuid));
                if (videoOptional.isEmpty() || videoOptional.get().getThumbnail() == null) {
                    return ResponseEntity.noContent().build();
                }

                byte[] imageBytes = videoOptional.get().getThumbnail();
                ByteArrayResource resource = new ByteArrayResource(imageBytes);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(imageBytes.length)
                        .body(resource);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        });
    }
}
