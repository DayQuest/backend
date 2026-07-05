package com.dayquest.userservice.services;

import com.dayquest.userservice.dto.ProfileDTO;
import com.dayquest.userservice.enums.Punishments;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.BadgeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {
    private static final String DEFAULT_PROFILE_PICTURE_URL = "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg";
    private static final String PROFILE_PICTURE_BASE_URL = "https://apiv2.dayquest.de/api/users/profilepicture/";
    private final FollowService followService;
    private final BadgeRepository badgeRepository;

    public UserService(FollowService followService, BadgeRepository badgeRepository) {
        this.followService = followService;
        this.badgeRepository = badgeRepository;
    }

    /**
     * Get the profile picture URL for a user.
     * Priority: MinIO URL > DB BLOB endpoint > Default
     */
    private String getProfilePictureUrl(User user) {
        // Prefer MinIO URL if available
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            return user.getProfilePictureUrl();
        }
        // Default avatar
        return DEFAULT_PROFILE_PICTURE_URL;
    }

    @Async
    @Cacheable(value = "userProfiles", key = "#userWithVideos.uuid", condition = "#requester == null")
    public CompletableFuture<ProfileDTO> createProfileDTO(User userWithVideos, User requester) {
        String profilePictureUrl = getProfilePictureUrl(userWithVideos);

        if (requester == null) {
            return CompletableFuture.supplyAsync(() -> new ProfileDTO(
                    userWithVideos.getUsername(),
                    profilePictureUrl,
                    null,
                    userWithVideos.getPunishment() == Punishments.BANNED,
                    userWithVideos.getFollowers(),
                    false,
                    badgeRepository.findBadgeIdsByUserId(userWithVideos.getUuid())
            ));
        }

        CompletableFuture<Boolean> isFollowingFuture =
                followService.isFollowing(requester.getUuid(), userWithVideos.getUuid());

        CompletableFuture<List<UUID>> badgesFuture =
                CompletableFuture.supplyAsync(() ->
                        badgeRepository.findBadgeIdsByUserId(userWithVideos.getUuid()));

        return isFollowingFuture.thenCombine(badgesFuture, (isFollowing, badges) ->
                new ProfileDTO(
                        userWithVideos.getUsername(),
                        profilePictureUrl,
                        null, // videos
                        userWithVideos.getPunishment() == Punishments.BANNED,
                        userWithVideos.getFollowers(),
                        isFollowing,
                        badges
                ));
    }
}
