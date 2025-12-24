package com.dayquest.social.repository;

import com.dayquest.social.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByVideoUuidAndDeletedFalseAndParentCommentUuidIsNull(UUID videoUuid, Pageable pageable);

    Page<Comment> findByParentCommentUuidAndDeletedFalse(UUID parentCommentUuid, Pageable pageable);

    Page<Comment> findByUserUuidAndDeletedFalse(UUID userUuid, Pageable pageable);

    long countByVideoUuidAndDeletedFalse(UUID videoUuid);
}
