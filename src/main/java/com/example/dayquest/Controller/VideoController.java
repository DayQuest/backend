package com.example.dayquest.Controller;

import com.example.dayquest.Repository.VideoRepository;
import com.example.dayquest.model.Video;
import com.example.dayquest.model.User;

import com.example.dayquest.Service.VideoService;
import com.example.dayquest.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import org.springframework.http.*;

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

    private final VideoService videoService;
    private final UserService userService;
    private final VideoRepository videoRepository;

    @Autowired
    private Cache<Integer, String> videoCache;

    public VideoController(VideoService videoService, UserService userService, VideoRepository videoRepository) {
        this.videoService = videoService;
        this.userService = userService;
        this.videoRepository = videoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Video>> getAllVideos() {
        List<Video> videos = videoService.getAllVideos();
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file,
                                              @RequestParam("title") String title,
                                              @RequestParam("description") String description) {
        try {
            videoService.uploadVideo(file, title, description);
            return ResponseEntity.ok("Video uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload video");
        }
    }
    @PostMapping("/delete")
    public ResponseEntity<String> deleteVideo(@RequestParam("id") Long id) {
        VideoService videoService = new VideoService();
        videoService.deleteVideo(id);
        return ResponseEntity.ok("Video deleted");
    }

    @GetMapping("/videos/{fileName:.+}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = videoService.loadVideoAsResource(fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }



    @PostMapping("/nextVid")
    public ResponseEntity<?> nextVid(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("User ID is missing");
        }

        Video video = videoService.getRandomVideo();
        if (video == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No videos available");
        }

        return ResponseEntity.ok(video);
    }



    @PostMapping("/{id}/upvote")
    public ResponseEntity<Video> upvoteVideo(@PathVariable Long id, @RequestBody String uuid) {
        try {
            UUID userUuid = UUID.fromString(uuid);
            User user = userService.getUserByUuid(userUuid);
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
    }
    @PostMapping("/{id}")
    public ResponseEntity<Video> getVideoById(@PathVariable Long id) {
        return ResponseEntity.ok(videoRepository.findById(id).orElse(null));
    }

    @PostMapping("/{id}/downvote")
    public ResponseEntity<Video> downvoteVideo(@PathVariable Long id, @RequestBody String uuid) {
        try {
            UUID userUuid = UUID.fromString(uuid);
            User user = userService.getUserByUuid(userUuid);
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
