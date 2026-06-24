package com.dayquest.userservice.services;

import com.dayquest.userservice.dto.ProfileDTO;
import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.BadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private FollowService followService;

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User requester;
    private UUID testUserUuid;
    private UUID requesterUuid;

    @BeforeEach
    void setUp() {
        testUserUuid = UUID.randomUUID();
        requesterUuid = UUID.randomUUID();

        testUser = new User();
        testUser.setUuid(testUserUuid);
        testUser.setUsername("testuser");
        testUser.setFollowers(100);
        testUser.setPunishment(Punishments.NONE);

        requester = new User();
        requester.setUuid(requesterUuid);
        requester.setUsername("requester");
    }

    @Test
    void createProfileDTO_WithRequester_ShouldReturnFullProfile() throws ExecutionException, InterruptedException {
        // Given
        boolean isFollowing = true;
        List<UUID> badges = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(followService.isFollowing(requesterUuid, testUserUuid))
                .thenReturn(CompletableFuture.completedFuture(isFollowing));
        when(badgeRepository.findBadgeIdsByUserId(testUserUuid))
                .thenReturn(badges);

        // When
        CompletableFuture<ProfileDTO> resultFuture = userService.createProfileDTO(testUser, requester);
        ProfileDTO result = resultFuture.get();

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(100, result.getFollowers());
        assertTrue(result.isFollowing());
        assertEquals(badges, result.getBadges());
        assertFalse(result.isBanned());

        verify(followService).isFollowing(requesterUuid, testUserUuid);
        verify(badgeRepository).findBadgeIdsByUserId(testUserUuid);
    }

    @Test
    void createProfileDTO_WithoutRequester_ShouldReturnProfileWithoutFollowStatus() throws ExecutionException, InterruptedException {
        // Given
        List<UUID> badges = List.of(UUID.randomUUID());

        // Use lenient because this might not be called significantly in some implementations,
        // but it is called inside the method.
        // However, since requester is null, followService.isFollowing should NOT be called.

        when(badgeRepository.findBadgeIdsByUserId(testUserUuid))
                .thenReturn(badges);

        // When
        CompletableFuture<ProfileDTO> resultFuture = userService.createProfileDTO(testUser, null);
        ProfileDTO result = resultFuture.get();

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertFalse(result.isFollowing());
        assertEquals(badges, result.getBadges());

        verify(followService, never()).isFollowing(any(), any());
        verify(badgeRepository).findBadgeIdsByUserId(testUserUuid);
    }

    @Test
    void createProfileDTO_WithProfilePicture_ShouldReturnCorrectUrl() throws ExecutionException, InterruptedException {
        // Given
        testUser.setProfilePicture(new byte[]{1, 2, 3});

        when(followService.isFollowing(requesterUuid, testUserUuid))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(badgeRepository.findBadgeIdsByUserId(testUserUuid))
                .thenReturn(List.of());

        // When
        CompletableFuture<ProfileDTO> resultFuture = userService.createProfileDTO(testUser, requester);
        ProfileDTO result = resultFuture.get();

        // Then
        assertTrue(result.getProfilePicture().contains("testuser"));
        assertTrue(result.getProfilePicture().contains("https://apiv2.dayquest.de/api/users/profilepicture/"));
    }

    @Test
    void createProfileDTO_WithoutProfilePicture_ShouldReturnDefaultUrl() throws ExecutionException, InterruptedException {
        // Given
        testUser.setProfilePicture(null);

        when(followService.isFollowing(requesterUuid, testUserUuid))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(badgeRepository.findBadgeIdsByUserId(testUserUuid))
                .thenReturn(List.of());

        // When
        CompletableFuture<ProfileDTO> resultFuture = userService.createProfileDTO(testUser, requester);
        ProfileDTO result = resultFuture.get();

        // Then
        assertTrue(result.getProfilePicture().contains("default-avatar"));
    }

    @Test
    void createProfileDTO_WithBannedUser_ShouldReturnBannedStatus() throws ExecutionException, InterruptedException {
        // Given
        testUser.setPunishment(Punishments.BANNED);

        when(followService.isFollowing(requesterUuid, testUserUuid))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(badgeRepository.findBadgeIdsByUserId(testUserUuid))
                .thenReturn(List.of());

        // When
        CompletableFuture<ProfileDTO> resultFuture = userService.createProfileDTO(testUser, requester);
        ProfileDTO result = resultFuture.get();

        // Then
        assertTrue(result.isBanned());
    }
}

