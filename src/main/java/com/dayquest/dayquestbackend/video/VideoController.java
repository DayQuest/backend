package com.dayquest.dayquestbackend.video;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.common.dto.UuidDTO;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.storage.Service.ThumbnailStorageService;
import com.dayquest.dayquestbackend.user.User;

import com.dayquest.dayquestbackend.user.UserRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.video.dto.VideoDTO;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.models.ViewedVideo;
import com.dayquest.dayquestbackend.video.models.ViewedVideoId;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import com.dayquest.dayquestbackend.video.repository.ViewedVideoRepository;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.*;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private QuestRepository questRepository;

    @Autowired
    private AsyncTaskExecutor delegatingSecurityContextAsyncTaskExecutor;

    @Autowired
    private ActivityUpdater activityUpdater;
    @Autowired
    private ThumbnailStorageService thumbnailStorageService;

    @Async
    @PostMapping
    public CompletableFuture<ResponseEntity<String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestHeader("Authorization") String token,
            @RequestParam("hashtags") List<String> hashtags) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token);
            Optional<User> user = Optional.ofNullable(userRepository.findByUsername(username));
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not find user with that UUID");
            }
            videoService.uploadVideo(file, title, description, user.get(), hashtags).join();
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

                if (user.isEmpty() || video.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
                for(int i = 0; i<video.get().getHashtags().size(); i++){
                    user.get().addLikedHashtag(video.get().getHashtags().get(i).getUuid());
                }
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
                for(int i = 0; i<video.get().getHashtags().size(); i++){
                    user.get().getLikedHashtags().remove(video.get().getHashtags().get(i).getUuid());
                }
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
                for(int i = 0; i<video.get().getHashtags().size(); i++){
                    user.get().getLikedHashtags().remove(video.get().getHashtags().get(i).getUuid());
                }
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
    public CompletableFuture<ResponseEntity<byte[]>> getDecodedImage(@PathVariable("uuid") String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Video> video = videoRepository.findById(UUID.fromString(uuid));
            return video.map(value -> ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnailStorageService.getThumbnail(uuid))).orElseGet(() -> ResponseEntity.notFound().build());

        });
    }
}
