package com.dayquest.social.controller;

import com.dayquest.social.dto.CommentDTO;
import com.dayquest.social.dto.CreateCommentDTO;
import com.dayquest.social.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comment Service", description = "Comment management endpoints")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Social Service - Comments running");
    }

    @GetMapping("/video/{videoUuid}")
    @Operation(summary = "Get comments for a video")
    public ResponseEntity<Map<String, Object>> getCommentsForVideo(
            @PathVariable UUID videoUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CommentDTO> commentPage = commentService.getCommentsForVideo(videoUuid, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("comments", commentPage.getContent());
        response.put("currentPage", commentPage.getNumber());
        response.put("totalItems", commentPage.getTotalElements());
        response.put("totalPages", commentPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{commentUuid}/replies")
    @Operation(summary = "Get replies for a comment")
    public ResponseEntity<Map<String, Object>> getReplies(
            @PathVariable UUID commentUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CommentDTO> replyPage = commentService.getReplies(commentUuid, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("replies", replyPage.getContent());
        response.put("currentPage", replyPage.getNumber());
        response.put("totalItems", replyPage.getTotalElements());
        response.put("totalPages", replyPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/video/{videoUuid}")
    @Operation(summary = "Create a comment on a video")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable UUID videoUuid,
            @RequestBody @Valid CreateCommentDTO dto,
            @RequestHeader("X-User-Id") String userId) {
        CommentDTO comment = commentService.createComment(videoUuid, dto, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @DeleteMapping("/{commentUuid}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentUuid,
            @RequestHeader("X-User-Id") String userId) {
        commentService.deleteComment(commentUuid, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentUuid}/like")
    @Operation(summary = "Like a comment")
    public ResponseEntity<String> likeComment(
            @PathVariable UUID commentUuid,
            @RequestHeader("X-User-Id") String userId) {
        commentService.likeComment(commentUuid, UUID.fromString(userId));
        return ResponseEntity.ok("Comment liked");
    }

    @GetMapping("/video/{videoUuid}/count")
    @Operation(summary = "Get comment count for a video")
    public ResponseEntity<Long> getCommentCount(@PathVariable UUID videoUuid) {
        long count = commentService.getCommentCount(videoUuid);
        return ResponseEntity.ok(count);
    }
}
