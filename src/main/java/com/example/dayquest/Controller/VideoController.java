package com.example.dayquest.Controller;

import com.example.dayquest.Repository.VideoRepository;
import com.example.dayquest.model.Video;
import com.example.dayquest.model.User;
import com.example.dayquest.VideoSelection;
import com.example.dayquest.Service.VideoService;
import com.example.dayquest.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
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

    @PostMapping("/nextVid")
    public ResponseEntity<?> nextVid(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("User ID is missing");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        List<Video> allVideos = videoService.getAllVideos();
        int currentVideoIndex = VideoSelection.nextVideo(allVideos.toArray(new Video[0]), new String[0], 10);

        if (currentVideoIndex >= 130 && currentVideoIndex + 20 < allVideos.size()) {
            // Preload the next 20 videos
            preloadVideos(allVideos.subList(currentVideoIndex + 1, currentVideoIndex + 21));
        }

        Video currentVideo = allVideos.get(currentVideoIndex);
        String cachedFilePath = videoCache.getIfPresent(currentVideo.getId().intValue());

        if (cachedFilePath == null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(currentVideo.getVideo64());
                File tempFile = File.createTempFile("video_" + currentVideo.getId(), ".mp4");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(decodedBytes);
                }
                cachedFilePath = tempFile.getAbsolutePath();
                videoCache.put(currentVideo.getId().intValue(), cachedFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error decoding video");
            }
        }

        String videoUrl = "/api/videos/stream/" + currentVideo.getId();
        currentVideo.setFilePath(videoUrl);

        return ResponseEntity.ok(currentVideo);
    }


    @Async
    public void preloadVideos(List<Video> videosToPreload) {
        for (Video video : videosToPreload) {
            String cachedFilePath = videoCache.getIfPresent(video.getId().intValue());
            if (cachedFilePath == null) {
                try {
                    byte[] decodedBytes = Base64.getDecoder().decode(video.getVideo64());
                    File tempFile = File.createTempFile("video_" + video.getId(), ".mp4");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(decodedBytes);
                    }
                    videoCache.put(video.getId().intValue(), tempFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @GetMapping("/stream/{id}")
    public ResponseEntity<ResourceRegion> streamVideo(@PathVariable int id, @RequestHeader HttpHeaders headers) throws IOException {
        String filePath = videoCache.getIfPresent(id);
        if (filePath == null) {
            return ResponseEntity.notFound().build();
        }

        UrlResource video = new UrlResource("file:" + filePath);
        ResourceRegion region = resourceRegion(video, headers);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    private ResourceRegion resourceRegion(UrlResource video, HttpHeaders headers) throws IOException {
        long contentLength = video.contentLength();
        HttpRange httpRange = headers.getRange().stream().findFirst().orElse(null);
        if (httpRange != null) {
            long start = httpRange.getRangeStart(contentLength);
            long end = httpRange.getRangeEnd(contentLength);
            long rangeLength = Math.min(1 * 1024 * 1024, end - start + 1);
            return new ResourceRegion(video, start, rangeLength);
        } else {
            long rangeLength = Math.min(1 * 1024 * 1024, contentLength);
            return new ResourceRegion(video, 0, rangeLength);
        }
    }

    @Async
    @PostMapping("/{id}/upvote")
    public ResponseEntity<Video> upvoteVideo(@PathVariable Long id, @RequestParam String uuid) {
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
    public ResponseEntity<Video> downvoteVideo(@PathVariable Long id, @RequestParam String uuid) {
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
