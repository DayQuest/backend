package com.dayquest.dayquestbackend.video;

import com.dayquest.dayquestbackend.user.User;

import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import org.springframework.http.*;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

  private static final int PRELOAD_COUNT = 5;

  @Autowired
  private VideoService videoService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private VideoRepository videoRepository;

  @Autowired
  private Cache<Integer, String> videoCache;

  @GetMapping
  public ResponseEntity<List<Video>> getAllVideos() {
    List<Video> videos = videoService.getAllVideos();
    return ResponseEntity.ok(videos);
  }

  @Async
  @PostMapping("/upload")
  public CompletableFuture<ResponseEntity<String>> uploadVideo(@RequestParam("file") MultipartFile file,
      @RequestParam("title") String title,
      @RequestParam("description") String description) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        videoService.uploadVideo(file, title, description);
        return ResponseEntity.ok("Video uploaded successfully");
      } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload video");
      }
    });
  }

  @Async
  @PostMapping("/delete")
  public CompletableFuture<ResponseEntity<String>> deleteVideo(@RequestParam("id") Long id) {
    return CompletableFuture.supplyAsync(() -> {
      VideoService videoService = new VideoService();
      videoService.deleteVideo(id);
      return ResponseEntity.ok("Video deleted");
    });
  }

  @Async
  @GetMapping("/videos/{fileName:.+}")
  public CompletableFuture<ResponseEntity<Resource>> serveVideo(@PathVariable String fileName,
      HttpServletRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      Resource resource = videoService.loadVideoAsResource(fileName);
      String contentType = null;
      try {
        contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
      } catch (IOException ignored) {
      }
      if (contentType == null) {
        contentType = "application/octet-stream";
      }
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "inline; filename=\"" + resource.getFilename() + "\"")
          .body(resource);
    });
  }


  @Async
  @PostMapping("/nextVid")
  public CompletableFuture<ResponseEntity<?>> nextVid(@RequestBody Map<String, Long> request) {
   return CompletableFuture.supplyAsync(() -> {
     Long userId = request.get("userId");
     if (userId == null) {
       return ResponseEntity.badRequest().body("User ID is missing");
     }

     Video video = videoService.getRandomVideo();
     if (video == null) {
       return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No videos available");
     }

     return ResponseEntity.ok(video);
   });
  }


  @Async
  @PostMapping("/{id}/upvote")
  public CompletableFuture<ResponseEntity<Video>> upvoteVideo(@PathVariable Long id, @RequestBody UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
          return ResponseEntity.badRequest().body(null);
        }
        if (user.getLikedVideos().contains(id)) {
          return ResponseEntity.badRequest().body(null);
        } else {
          user.getLikedVideos().add(id);
          Video updatedVideo = videoService.upvoteVideo(id);
          return ResponseEntity.ok(updatedVideo);
        }
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(null);
      }
    });
  }

  @Async
  @PostMapping("/{id}")
  public CompletableFuture<ResponseEntity<Video>> getVideoById(@PathVariable Long id) {
    return CompletableFuture.supplyAsync(() -> {
      if (videoRepository.findById(id).isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      return videoRepository.findById(id).get();
    });
  }

  @Async
  @PostMapping("/{id}/downvote")
  public ResponseEntity<Video> downvoteVideo(@PathVariable Long id, @RequestBody UUID uuid) {
    try {
      User user = userRepository.findByUuid(uuid);
      if (user == null) {
        return ResponseEntity.badRequest().body(null);
      }
      if (user.getDislikedVideos().contains(id)) {
        return ResponseEntity.badRequest().body(null);
      } else {
        user.getDislikedVideos().add(id);
        Video updatedVideo = videoService.downvoteVideo(id);
        return ResponseEntity.ok(updatedVideo);
      }
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(null);
    }
  }
}
