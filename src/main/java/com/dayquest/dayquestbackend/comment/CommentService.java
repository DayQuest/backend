package com.dayquest.dayquestbackend.comment;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.video.Video;
import com.dayquest.dayquestbackend.video.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    public CompletableFuture<ResponseEntity<?>> createComment(String comment, UUID userId, UUID postId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userId);
            Optional<Video> post = videoRepository.findById(postId);

            if (user.isEmpty() || post.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (comment.isEmpty() || comment.length() > 500) {
                return ResponseEntity.badRequest().build();
            }

            Comment newComment = new Comment();
            newComment.setContent(comment);
            newComment.setUsername(user.get().getUsername());
            newComment.setVideoId(postId);
            commentRepository.save(newComment);
            return ResponseEntity.ok().build();
        });
    }

    public CompletableFuture<ResponseEntity<?>> answerComment(String comment, UUID userId, UUID commentId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userId);
            Optional<Comment> parentComment = commentRepository.findById(commentId);

            if (user.isEmpty() || parentComment.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (comment.isEmpty() || comment.length() > 500) {
                return ResponseEntity.badRequest().build();
            }

            Comment newComment = new Comment();
            newComment.setContent(comment);
            newComment.setUsername(user.get().getUsername());
            newComment.setVideoId(parentComment.get().getVideoId());
            newComment.setAnswer(true);
            newComment.setParentComment(parentComment.get());
            commentRepository.save(newComment);
            return ResponseEntity.ok().build();
        });
    }
}
