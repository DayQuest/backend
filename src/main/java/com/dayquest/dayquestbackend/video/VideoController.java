package com.dayquest.dayquestbackend.video;

import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.user.User;

import com.dayquest.dayquestbackend.user.UserRepository;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.*;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.List;
import java.util.UUID;
import java.security.Principal;
import java.util.concurrent.Executor;

import org.slf4j.Logger;

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
  private Cache<Integer, String> videoCache;
    @Autowired
    private QuestRepository questRepository;

  @Autowired
  private AsyncTaskExecutor delegatingSecurityContextAsyncTaskExecutor;
  ;

  @Async
  @GetMapping

  public CompletableFuture<ResponseEntity<List<Video>>> getAllVideos() {
    return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(videoRepository.findAll()));
  }

  @Async
  @PostMapping("/upload")
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

      String path = videoService.uploadVideo(file, title, description, user.get()).join();

      if (path == null) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Failed to upload video due to internal error");
      }

      return ResponseEntity.ok("Uploaded");
    });
  }

  @Async
  @PostMapping("/delete")
  public CompletableFuture<ResponseEntity<String>> deleteVideo(@RequestParam UUID uuid) {
    return videoService.deleteVideo(uuid);
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
              return ResponseEntity.ok(createVideoDTO(video, user));
            }))
            .orElseGet(() -> CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
            ));
  }

  private VideoDTO createVideoDTO(Video video, User user) {
    VideoDTO videoDTO = new VideoDTO(
            video.getTitle(),
            video.getDescription(),
            video.getUpVotes(),
            video.getDownVotes(),
            video.getUser().getUsername(),
            video.getFilePath(),
            null,
            questRepository.findById(video.getQuestUuid()).orElse(null),
            video.getUuid()
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

        // Verify the user matches the authenticated user
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
        userRepository.save(user.get());
        return videoService.likeVideo(uuid).join();
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

        if(user.get().getLikedVideos().contains(uuid)) {
            user.get().getLikedVideos().remove(uuid);
            video.get().setUpVotes(video.get().getUpVotes() - 1);
            videoRepository.save(video.get());

        }

      user.get().getDislikedVideos().add(uuid);
        userRepository.save(user.get());
      return videoService.dislikeVideo(uuid).join();
    });
  }

  @Async
  @PostMapping("/{uuid}")
  public CompletableFuture<ResponseEntity<Video>> getVideoById(@PathVariable UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      if (videoRepository.findById(uuid).isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok(videoRepository.findById(uuid).get());
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
