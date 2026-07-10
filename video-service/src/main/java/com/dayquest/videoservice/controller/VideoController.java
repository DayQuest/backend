package com.dayquest.videoservice.controller;

import com.dayquest.videoservice.dto.VideoDTO;
import com.dayquest.videoservice.dto.VideoUploadRequest;
import com.dayquest.videoservice.model.Video;
import com.dayquest.videoservice.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/videos")
@Tag(name = "Videos", description = "Video management endpoints")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new video")
    public CompletableFuture<ResponseEntity<Object>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "questUuid", required = false) UUID questUuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        VideoUploadRequest request = new VideoUploadRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setQuestUuid(questUuid);

        return videoService.uploadVideo(file, request, userUuid)
                .thenApply(video -> ResponseEntity.ok((Object) videoService.toDTO(video, userUuid)))
                .exceptionally(ex -> {
                    logger.error("Upload failed", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Upload failed"));
                });
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get video by UUID")
    public ResponseEntity<?> getVideo(
            @PathVariable UUID uuid,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        UUID userUuid = userIdHeader != null ? UUID.fromString(userIdHeader) : null;

        Optional<Video> videoOpt = videoService.getVideo(uuid);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(videoService.toDTO(videoOpt.get(), userUuid));
    }

    @GetMapping
    @Operation(summary = "Get videos with filters and sorting")
    public ResponseEntity<Page<VideoDTO>> getVideos(
            @RequestParam(required = false) UUID userUuid,
            @RequestParam(required = false) UUID questUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        UUID currentUserUuid = userIdHeader != null ? UUID.fromString(userIdHeader) : null;

        Sort sort = Sort.by(sortDirection, sortBy);
        Page<Video> videos = videoService.getVideos(userUuid, questUuid, PageRequest.of(page, size, sort));

        Page<VideoDTO> dtos = videos.map(v -> videoService.toDTO(v, currentUserUuid));

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/next")
    @Operation(summary = "Get next video for user feed")
    public ResponseEntity<?> getNextVideo(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userUuid = UUID.fromString(userIdHeader);

        Optional<Video> videoOpt = videoService.getNextVideo(userUuid);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(videoService.toDTO(videoOpt.get(), userUuid));
    }

    @DeleteMapping("/{uuid}")
    @Operation(
            summary = "Delete a video",
            description = "Soft-deletes a video and removes associated ratings and view records. " +
                    "Cross-service comment cleanup (social-service) is handled asynchronously " +
                    "via RabbitMQ — planned future enhancement."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Video deleted"),
            @ApiResponse(responseCode = "403", description = "Not the video owner")
    })
    public ResponseEntity<?> deleteVideo(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        boolean deleted = videoService.deleteVideo(uuid, userUuid);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot delete video"));
        }

        return ResponseEntity.ok(Map.of("message", "Video deleted"));
    }

    @PostMapping("/{uuid}/vote")
    @Operation(
            summary = "Vote on a video (upvote/downvote)",
            description = "Casts a vote (upvote or downvote) on a video. Pass `isUpvote=true` to upvote or `isUpvote=false` to downvote. " +
                    "Voting again with the same type removes the existing vote (toggle)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vote recorded"),
            @ApiResponse(responseCode = "404", description = "Video not found")
    })
    public ResponseEntity<?> voteVideo(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam boolean isUpvote) {

        UUID userUuid = UUID.fromString(userIdHeader);
        boolean success = isUpvote
                ? videoService.upvoteVideo(uuid, userUuid)
                : videoService.downvoteVideo(uuid, userUuid);

        if (!success) {
            return ResponseEntity.notFound().build();
        }

        String voteType = isUpvote ? "upvote" : "downvote";
        return ResponseEntity.ok(Map.of("message", "Vote recorded", "voteType", voteType));
    }

    @PostMapping("/{uuid}/view")
    @Operation(summary = "Mark video as viewed")
    public ResponseEntity<?> markAsViewed(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);
        videoService.markAsViewed(userUuid, uuid);

        return ResponseEntity.ok(Map.of("message", "Marked as viewed"));
    }
}