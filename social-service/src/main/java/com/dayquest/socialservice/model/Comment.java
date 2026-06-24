package com.dayquest.socialservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_entity", columnList = "entityId, entityType"),
    @Index(name = "idx_comment_user", columnList = "userUuid"),
    @Index(name = "idx_comment_parent", columnList = "parentCommentId"),
    @Index(name = "idx_comment_created", columnList = "createdAt DESC"),
    @Index(name = "idx_comment_thread", columnList = "entityId, entityType, createdAt"),
    @Index(name = "idx_comment_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private UUID entityId; // Can be video UUID or quest UUID

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CommentEntityType entityType;

    @Column(nullable = false)
    private UUID userUuid;

    private String username;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    private int likes = 0;

    // Depth for limiting nesting
    @Column(nullable = false)
    private int depth = 0;

    // Reply count denormalized for display
    @Column(nullable = false)
    private int replyCount = 0;

    // Soft delete support
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
