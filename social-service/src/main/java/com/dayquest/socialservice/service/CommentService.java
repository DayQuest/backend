package com.dayquest.socialservice.service;

import com.dayquest.socialservice.dto.CommentDTO;
import com.dayquest.socialservice.dto.CreateCommentRequest;
import com.dayquest.socialservice.model.Comment;
import com.dayquest.socialservice.model.CommentEntityType;
import com.dayquest.socialservice.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public Comment createComment(CreateCommentRequest request, UUID userUuid, String username) {
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setEntityId(request.getEntityId());
        comment.setEntityType(request.getEntityType());
        comment.setUserUuid(userUuid);
        comment.setUsername(username);

        if (request.getParentCommentId() != null) {
            Optional<Comment> parentOpt = commentRepository.findById(request.getParentCommentId());
            parentOpt.ifPresent(comment::setParentComment);
        }

        return commentRepository.save(comment);
    }

    public Page<Comment> getComments(UUID entityId, CommentEntityType entityType, Pageable pageable) {
        return commentRepository.findByEntityIdAndEntityTypeAndParentCommentIsNull(
                entityId, entityType, pageable);
    }

    public Optional<Comment> getComment(UUID commentId) {
        return commentRepository.findById(commentId);
    }

    @Transactional
    public boolean updateComment(UUID commentId, String content, UUID userUuid) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return false;
        }

        Comment comment = commentOpt.get();
        if (!comment.getUserUuid().equals(userUuid)) {
            return false;
        }

        comment.setContent(content);
        commentRepository.save(comment);
        return true;
    }

    //TODO: Just mark as deleted for moderation purposes and fully delete in 7 days also process answers
    @Transactional
    public boolean deleteComment(UUID commentId, UUID userUuid) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return false;
        }

        Comment comment = commentOpt.get();
        if (!comment.getUserUuid().equals(userUuid)) {
            return false;
        }

        commentRepository.delete(comment);
        return true;
    }

    public long getCommentCount(UUID entityId, CommentEntityType entityType) {
        return commentRepository.countByEntityIdAndEntityType(entityId, entityType);
    }

    public CommentDTO toDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setEntityId(comment.getEntityId());
        dto.setEntityType(comment.getEntityType());
        dto.setUserUuid(comment.getUserUuid());
        dto.setUsername(comment.getUsername());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setLikes(comment.getLikes());
        dto.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);
        dto.setReplies(comment.getReplies().stream().map(this::toDTO).collect(Collectors.toList()));
        return dto;
    }
}

