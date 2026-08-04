package com.dayquest.userservice.services;

import com.dayquest.userservice.ids.FollowId;
import com.dayquest.userservice.models.Follow;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.FollowRepository;
import com.dayquest.userservice.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Follow Service Tests")
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserServiceJwtService jwtService;

    @InjectMocks
    private FollowService followService;

    private User follower;
    private User followed;
    private UUID followerId;
    private UUID followedId;

    private static final String TOKEN = "Bearer test.token";

    @BeforeEach
    void setUp() {
        followerId = UUID.randomUUID();
        followedId = UUID.randomUUID();

        follower = new User();
        follower.setUuid(followerId);
        follower.setUsername("follower");

        followed = new User();
        followed.setUuid(followedId);
        followed.setUsername("followed");
    }

    @Test
    @DisplayName("Should follow user successfully")
    void shouldFollowUser() {
        when(jwtService.extractUserId(TOKEN)).thenReturn(followerId);
        when(followRepository.findById(any(FollowId.class))).thenReturn(Optional.empty());
        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(followed));

        CompletableFuture<ResponseEntity<String>> result = followService.followUser(TOKEN, followedId);

        ResponseEntity<String> response = result.join();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("User followed successfully", response.getBody());

        verify(followRepository).save(any(Follow.class));
    }

    @Test
    @DisplayName("Should unfollow user successfully")
    void shouldUnfollowUser() {
        when(jwtService.extractUserId(TOKEN)).thenReturn(followerId);
        when(followRepository.findById(any(FollowId.class))).thenReturn(Optional.of(new Follow(follower, followed)));

        CompletableFuture<ResponseEntity<String>> result = followService.unfollowUser(TOKEN, followedId);

        ResponseEntity<String> response = result.join();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("User unfollowed successfully", response.getBody());

        verify(followRepository).delete(any(Follow.class));
    }
}
