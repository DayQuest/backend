package com.dayquest.video.controller;

import com.dayquest.video.dto.UploadVideoDTO;
import com.dayquest.video.dto.VideoResponseDTO;
import com.dayquest.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
@Tag(name = "Video Service", description = "Video management endpoints")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Video Service is running");
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get video by UUID")
    public ResponseEntity<VideoResponseDTO> getVideoById(@PathVariable UUID uuid) {
        VideoResponseDTO video = videoService.getVideoById(uuid);
        return ResponseEntity.ok(video);
    }

    @GetMapping
    @Operation(summary = "Get all videos")
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VideoResponseDTO> videoPage = videoService.getAllVideos(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoPage.getContent());
        response.put("currentPage", videoPage.getNumber());
        response.put("totalItems", videoPage.getTotalElements());
        response.put("totalPages", videoPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Upload a new video")
    public ResponseEntity<VideoResponseDTO> uploadVideo(
            @RequestPart("metadata") @Valid UploadVideoDTO dto,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) throws IOException {
        VideoResponseDTO video = videoService.uploadVideo(dto, UUID.fromString(userId), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a video")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID uuid) {
        videoService.deleteVideo(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uuid}/upvote")
    @Operation(summary = "Upvote a video")
    public ResponseEntity<String> upvoteVideo(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userId) {
        videoService.upvoteVideo(uuid);
        return ResponseEntity.ok("Video upvoted");
    }

    @PostMapping("/{uuid}/downvote")
    @Operation(summary = "Downvote a video")
    public ResponseEntity<String> downvoteVideo(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userId) {
        videoService.downvoteVideo(uuid);
        return ResponseEntity.ok("Video downvoted");
    }

    @PostMapping("/{uuid}/view")
    @Operation(summary = "Increment video view count")
    public ResponseEntity<Void> incrementViews(@PathVariable UUID uuid) {
        videoService.incrementViews(uuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userUuid}")
    @Operation(summary = "Get videos by user")
    public ResponseEntity<Map<String, Object>> getVideosByUser(
            @PathVariable UUID userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VideoResponseDTO> videoPage = videoService.getVideosByUser(userUuid, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoPage.getContent());
        response.put("currentPage", videoPage.getNumber());
        response.put("totalItems", videoPage.getTotalElements());
        response.put("totalPages", videoPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/quest/{questUuid}")
    @Operation(summary = "Get videos by quest")
    public ResponseEntity<Map<String, Object>> getVideosByQuest(
            @PathVariable UUID questUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VideoResponseDTO> videoPage = videoService.getVideosByQuest(questUuid, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoPage.getContent());
        response.put("currentPage", videoPage.getNumber());
        response.put("totalItems", videoPage.getTotalElements());
        response.put("totalPages", videoPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent videos")
    public ResponseEntity<List<VideoResponseDTO>> getRecentVideos(
            @RequestParam(defaultValue = "10") int limit) {
        List<VideoResponseDTO> videos = videoService.getRecentVideos(limit);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/top")
    @Operation(summary = "Get top rated videos")
    public ResponseEntity<List<VideoResponseDTO>> getTopRatedVideos(
            @RequestParam(defaultValue = "10") int limit) {
        List<VideoResponseDTO> videos = videoService.getTopRatedVideos(limit);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/search")
    @Operation(summary = "Search videos by title")
    public ResponseEntity<Map<String, Object>> searchVideos(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<VideoResponseDTO> videoPage = videoService.searchVideos(query, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoPage.getContent());
        response.put("currentPage", videoPage.getNumber());
        response.put("totalItems", videoPage.getTotalElements());
        response.put("totalPages", videoPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // Internal endpoint for social service to increment comment count
    @PostMapping("/internal/{uuid}/increment-comments")
    @Operation(summary = "Increment comment count (internal)")
    public ResponseEntity<Void> incrementComments(@PathVariable UUID uuid) {
        videoService.incrementComments(uuid);
        return ResponseEntity.ok().build();
    }
}
