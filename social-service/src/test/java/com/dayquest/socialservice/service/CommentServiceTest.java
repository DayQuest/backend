package com.dayquest.socialservice.service;

import com.dayquest.socialservice.dto.CreateCommentRequest;
import com.dayquest.socialservice.model.Comment;
import com.dayquest.socialservice.model.CommentEntityType;
import com.dayquest.socialservice.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Comment Service Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private Comment testComment;
    private UUID testCommentId;
    private UUID testUserId;
    private UUID testEntityId;
    private CreateCommentRequest createRequest;

    @BeforeEach
    void setUp() {
        testCommentId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testEntityId = UUID.randomUUID();

        testComment = new Comment();
        testComment.setId(testCommentId);
        testComment.setUserUuid(testUserId);
        testComment.setContent("Test comment content");
        testComment.setEntityId(testEntityId);
        testComment.setEntityType(CommentEntityType.VIDEO);
        testComment.setUsername("testuser");

        createRequest = new CreateCommentRequest();
        createRequest.setContent("Test comment content");
        createRequest.setEntityId(testEntityId);
        createRequest.setEntityType(CommentEntityType.VIDEO);
    }

    @Test
    @DisplayName("Should create comment successfully")
    void shouldCreateComment() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(testCommentId);
            return c;
        });

        Comment result = commentService.createComment(createRequest, testUserId, "testuser");

        assertNotNull(result);
        assertEquals("Test comment content", result.getContent());
        assertEquals(testUserId, result.getUserUuid());
        assertEquals(testEntityId, result.getEntityId());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should create comment with parent comment")
    void shouldCreateCommentWithParent() {
        createRequest.setParentCommentId(testCommentId);
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.createComment(createRequest, testUserId, "testuser");

        assertNotNull(result);
        assertEquals(testComment, result.getParentComment());
        verify(commentRepository).findById(testCommentId);
    }

    @Test
    @DisplayName("Should get comment by ID")
    void shouldGetCommentById() {
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(testComment));

        Optional<Comment> result = commentService.getComment(testCommentId);

        assertTrue(result.isPresent());
        assertEquals(testComment.getContent(), result.get().getContent());
    }

    @Test
    @DisplayName("Should return empty when comment not found")
    void shouldReturnEmptyWhenCommentNotFound() {
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.empty());

        Optional<Comment> result = commentService.getComment(testCommentId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get comments for entity")
    void shouldGetCommentsForEntity() {
        Page<Comment> commentPage = new PageImpl<>(List.of(testComment));
        when(commentRepository.findByEntityIdAndEntityTypeAndParentCommentIsNull(
                eq(testEntityId), eq(CommentEntityType.VIDEO), any(Pageable.class)))
                .thenReturn(commentPage);

        Page<Comment> result = commentService.getComments(testEntityId, CommentEntityType.VIDEO, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(testComment.getContent(), result.getContent().get(0).getContent());
    }

    @Test
    @DisplayName("Should update comment successfully")
    void shouldUpdateComment() {
        String newContent = "Updated content";
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        boolean result = commentService.updateComment(testCommentId, newContent, testUserId);

        assertTrue(result);
        assertEquals(newContent, testComment.getContent());
        verify(commentRepository).save(testComment);
    }

    @Test
    @DisplayName("Should not update comment when user is not owner")
    void shouldNotUpdateCommentWhenUserIsNotOwner() {
        UUID differentUserId = UUID.randomUUID();
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(testComment));

        boolean result = commentService.updateComment(testCommentId, "New content", differentUserId);

        assertFalse(result);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not update comment when not found")
    void shouldNotUpdateCommentWhenNotFound() {
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.empty());

        boolean result = commentService.updateComment(testCommentId, "New content", testUserId);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should delete comment successfully")
    void shouldDeleteComment() {
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(testComment));
        doNothing().when(commentRepository).delete(testComment);

        boolean result = commentService.deleteComment(testCommentId, testUserId);

        assertTrue(result);
        verify(commentRepository).delete(testComment);
    }

    @Test
    @DisplayName("Should not delete comment when user is not owner")
    void shouldNotDeleteCommentWhenUserIsNotOwner() {
        UUID differentUserId = UUID.randomUUID();
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(testComment));

        boolean result = commentService.deleteComment(testCommentId, differentUserId);

        assertFalse(result);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should not delete comment when not found")
    void shouldNotDeleteCommentWhenNotFound() {
        when(commentRepository.findById(testCommentId)).thenReturn(Optional.empty());

        boolean result = commentService.deleteComment(testCommentId, testUserId);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should get comment count")
    void shouldGetCommentCount() {
        when(commentRepository.countByEntityIdAndEntityType(testEntityId, CommentEntityType.VIDEO))
                .thenReturn(5L);

        long count = commentService.getCommentCount(testEntityId, CommentEntityType.VIDEO);

        assertEquals(5L, count);
    }
}
