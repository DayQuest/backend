package com.dayquest.dayquestbackend.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;
    @Autowired
    private CommentRepository commentRepository;

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<?>> createComment(@RequestBody CommentDTO commentDTO) {
        return commentService.createComment(commentDTO.getContent(), commentDTO.getUserId(), commentDTO.getVideoId());
    }

    @PostMapping("/answer")
    @Async
    public CompletableFuture<ResponseEntity<?>> answerComment(@RequestBody AnswerDTO answerDTO) {
        return commentService.answerComment(answerDTO.getContent(), answerDTO.getUserId(), answerDTO.getCommentId());
    }

    @PostMapping("/get/{videoId}")
    @Async
    public CompletableFuture<ResponseEntity<?>> getComments(@PathVariable UUID videoId) {
        return CompletableFuture.supplyAsync(() -> {
            return ResponseEntity.ok(commentRepository.findByVideoIdAndIsAnswerFalse(videoId));
        });
    }

}
