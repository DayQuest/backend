package com.dayquest.socialservice.repository;

import com.dayquest.socialservice.model.Comment;
import com.dayquest.socialservice.model.CommentEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {


    Page<Comment> findByEntityIdAndEntityTypeAndParentCommentIsNullAndDeletedFalse(
            UUID entityId, CommentEntityType entityType, Pageable pageable);

    List<Comment> findByParentCommentId(UUID parentCommentId);


    long countByEntityIdAndEntityTypeAndDeletedFalse(UUID entityId, CommentEntityType entityType);

    void deleteByUserUuid(UUID userUuid);


    List<Comment> findByDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}