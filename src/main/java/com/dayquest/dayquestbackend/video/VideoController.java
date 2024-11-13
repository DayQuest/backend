package com.dayquest.dayquestbackend.video;

import com.dayquest.dayquestbackend.user.User;

import com.dayquest.dayquestbackend.user.UserRepository;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.ByteArrayResource;
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
  private Cache<Integer, String> videoCache;

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
            .map(user -> videoService.getRandomVideo()
                    .thenApply(video -> {
                      if (video == null) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("No video found");
                      }
                      VideoDTO videoDTO = new VideoDTO(video.getTitle(), video.getDescription(), video.getUpVotes(), video.getDownVotes(), video.getUser().getUsername(), video.getFilePath(), null);
                      return ResponseEntity.ok(videoDTO);
                    }))
            .orElseGet(() -> CompletableFuture.completedFuture(
                    ResponseEntity.notFound().build()));
  }


  @Async
  @PostMapping("/{uuid}/like")
  public CompletableFuture<ResponseEntity<Video>> likeVideo(@PathVariable UUID uuid,
      @RequestBody UuidDTO userUuid) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(UUID.fromString(userUuid.getUuid()));
      if (user.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      if (user.get().getLikedVideos().contains(uuid)) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
      }

      user.get().getLikedVideos().add(uuid);
      return videoService.likeVideo(uuid).join();
    });
  }

  @Async
  @PostMapping("/{uuid}/dislike")
  public CompletableFuture<ResponseEntity<Video>> dislikeVideo(@PathVariable UUID uuid,
      @RequestBody UUID userUuid) {
    return CompletableFuture.supplyAsync(() -> {
      Optional<User> user = userRepository.findById(userUuid);
      if (user.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      if (user.get().getDislikedVideos().contains(uuid)) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
      }

      user.get().getDislikedQuests().add(uuid);
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
