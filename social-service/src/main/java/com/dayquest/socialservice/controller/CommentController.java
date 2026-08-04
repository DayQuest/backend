package com.dayquest.socialservice.controller;

import com.dayquest.socialservice.dto.CommentDTO;
import com.dayquest.socialservice.dto.CreateCommentRequest;
import com.dayquest.socialservice.dto.UpdateCommentRequest;
import com.dayquest.socialservice.model.Comment;
import com.dayquest.socialservice.model.CommentEntityType;
import com.dayquest.socialservice.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comments", description = "Comment management endpoints")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }


    //TODO: Increment the Comment count of the video
    @PostMapping
    @Operation(summary = "Create a new comment")
    public ResponseEntity<CommentDTO> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader("X-User-Id") UUID userUuid,
            @RequestHeader("X-Username") String username) {

        Comment comment = commentService.createComment(request, userUuid, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.toDTO(comment));
    }

    @GetMapping("/video/{videoId}")
    @Operation(summary = "Get comments for a video")
    public ResponseEntity<Page<CommentDTO>> getVideoComments(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Comment> comments = commentService.getComments(
                videoId, CommentEntityType.VIDEO,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return ResponseEntity.ok(comments.map(commentService::toDTO));
    }

    @GetMapping("/quest/{questId}")
    @Operation(summary = "Get comments for a quest")
    public ResponseEntity<Page<CommentDTO>> getQuestComments(
            @PathVariable UUID questId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Comment> comments = commentService.getComments(
                questId, CommentEntityType.QUEST,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return ResponseEntity.ok(comments.map(commentService::toDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a comment by ID")
    public ResponseEntity<CommentDTO> getComment(@PathVariable UUID id) {
        return commentService.getComment(id)
                .map(comment -> ResponseEntity.ok(commentService.toDTO(comment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a comment")
    public ResponseEntity<?> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest body,
            @RequestHeader("X-User-Id") UUID userUuid) {

        String content = body.getContent();

        boolean updated = commentService.updateComment(id, content, userUuid);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot update comment"));
        }

        return ResponseEntity.ok(Map.of("message", "Comment updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<?> deleteComment(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userUuid) {

        boolean deleted = commentService.deleteComment(id, userUuid);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot delete comment"));
        }

        return ResponseEntity.ok(Map.of("message", "Comment deleted"));
    }

    @GetMapping("/count/video/{videoId}")
    @Operation(summary = "Get comment count for a video")
    public ResponseEntity<Map<String, Long>> getVideoCommentCount(@PathVariable UUID videoId) {
        long count = commentService.getCommentCount(videoId, CommentEntityType.VIDEO);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/count/quest/{questId}")
    @Operation(summary = "Get comment count for a quest")
    public ResponseEntity<Map<String, Long>> getQuestCommentCount(@PathVariable UUID questId) {
        long count = commentService.getCommentCount(questId, CommentEntityType.QUEST);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
