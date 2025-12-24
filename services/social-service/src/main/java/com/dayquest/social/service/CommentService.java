package com.dayquest.social.service;

import com.dayquest.common.exception.ResourceNotFoundException;
import com.dayquest.social.dto.CommentDTO;
import com.dayquest.social.dto.CreateCommentDTO;
import com.dayquest.social.model.Comment;
import com.dayquest.social.model.NotificationType;
import com.dayquest.social.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
    }

    public Page<CommentDTO> getCommentsForVideo(UUID videoUuid, int page, int size) {
        Page<Comment> comments = commentRepository.findByVideoUuidAndDeletedFalseAndParentCommentUuidIsNull(
                videoUuid, PageRequest.of(page, size));
        return comments.map(CommentDTO::new);
    }

    public Page<CommentDTO> getReplies(UUID parentCommentUuid, int page, int size) {
        Page<Comment> replies = commentRepository.findByParentCommentUuidAndDeletedFalse(
                parentCommentUuid, PageRequest.of(page, size));
        return replies.map(CommentDTO::new);
    }

    @Transactional
    public CommentDTO createComment(UUID videoUuid, CreateCommentDTO dto, UUID userUuid) {
        Comment comment = new Comment(dto.getContent(), videoUuid, userUuid);
        comment.setParentCommentUuid(dto.getParentCommentUuid());
        comment = commentRepository.save(comment);

        // If it's a reply, increment parent comment's reply count and notify
        if (dto.getParentCommentUuid() != null) {
            Comment parentComment = commentRepository.findById(dto.getParentCommentUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            parentComment.setReplies(parentComment.getReplies() + 1);
            commentRepository.save(parentComment);

            // Notify parent comment author
            if (!parentComment.getUserUuid().equals(userUuid)) {
                notificationService.createNotification(
                        parentComment.getUserUuid(),
                        userUuid,
                        NotificationType.REPLY,
                        "replied to your comment",
                        comment.getUuid()
                );
            }
        }

        // TODO: Notify video owner via Video Service

        return new CommentDTO(comment);
    }

    @Transactional
    public void deleteComment(UUID commentUuid, UUID userUuid) {
        Comment comment = commentRepository.findById(commentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUserUuid().equals(userUuid)) {
            throw new IllegalArgumentException("Cannot delete another user's comment");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    @Transactional
    public void likeComment(UUID commentUuid, UUID userUuid) {
        Comment comment = commentRepository.findById(commentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setLikes(comment.getLikes() + 1);
        commentRepository.save(comment);

        // Notify comment author
        if (!comment.getUserUuid().equals(userUuid)) {
            notificationService.createNotification(
                    comment.getUserUuid(),
                    userUuid,
                    NotificationType.LIKE,
                    "liked your comment",
                    comment.getUuid()
            );
        }
    }

    public long getCommentCount(UUID videoUuid) {
        return commentRepository.countByVideoUuidAndDeletedFalse(videoUuid);
    }
}
