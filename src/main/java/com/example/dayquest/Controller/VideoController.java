package com.example.dayquest.Controller;

import com.example.dayquest.Repository.VideoRepository;
import com.example.dayquest.model.Video;
import com.example.dayquest.model.User;
import com.example.dayquest.VideoSelection;
import com.example.dayquest.Service.VideoService;
import com.example.dayquest.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private final VideoService videoService;
    private final UserService userService;
    private final VideoRepository videoRepository;

    @Autowired
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
    public ResponseEntity<Video> nextVid(@RequestBody Map<String, Long> request) {
        Long id = request.get("userId");
        if (id == null) {
            return ResponseEntity.badRequest().body(null);
        }

        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        System.out.println(user.getUsername());
        String[] likedHashtags = user.getLikedHashtags();
        int vidIndex = VideoSelection.nextVideo(videoService.getAllVideos().toArray(new Video[0]),
                likedHashtags == null ? new String[0] : likedHashtags, 10);

        return ResponseEntity.ok(videoService.getAllVideos().toArray(new Video[0])[vidIndex]);
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<Video> upvoteVideo(@PathVariable Long id) {
        Video updatedVideo = videoService.upvoteVideo(id);
        return ResponseEntity.ok(updatedVideo);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Video> getVideoById(@PathVariable Long id) {
        return ResponseEntity.ok(videoRepository.findById(id).orElse(null));
    }

    @PostMapping("/{id}/downvote")
    public ResponseEntity<Video> downvoteVideo(@PathVariable Long id) {
        Video updatedVideo = videoService.downvoteVideo(id);
        return ResponseEntity.ok(updatedVideo);
    }
}
