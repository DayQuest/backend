package com.dayquest.userservice.services;

import com.dayquest.userservice.dto.FollowUserDTO;
import com.dayquest.userservice.ids.FollowId;
import com.dayquest.userservice.models.Follow;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.FollowRepository;
import com.dayquest.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
public class FollowService {
    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserServiceJwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Async
    public CompletableFuture<ResponseEntity<String>> followUser(String token, UUID userToFollowId) {
        UUID userId = jwtService.extractUserId(token);
        FollowId followId = new FollowId(userId, userToFollowId);
        Follow existingFollow = followRepository.findById(followId).orElse(null);

        if (existingFollow != null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("Already following this user"));
        }
        if (userId == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("Invalid token"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }

        User userToFollow = userRepository.findById(userToFollowId).orElse(null);
        if (userToFollow == null) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }

        Follow follow = new Follow(user, userToFollow);
        followRepository.save(follow);
        return CompletableFuture.completedFuture(ResponseEntity.ok("User followed successfully"));
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> unfollowUser(String token, UUID userToUnfollowId) {
        UUID userId = jwtService.extractUserId(token);
        FollowId followId = new FollowId(userId, userToUnfollowId);

        Follow follow = followRepository.findById(followId).orElse(null);
        if (follow == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("Not following this user"));
        }
        followRepository.delete(follow);
        return CompletableFuture.completedFuture(ResponseEntity.ok("User unfollowed successfully"));
    }

    @Async
    public CompletableFuture<Boolean> isFollowing(UUID userId, UUID userToCheckId) {
        FollowId followId = new FollowId(userId, userToCheckId);
        return CompletableFuture.completedFuture(followRepository.existsById(followId));
    }

    /**
     * Returns a page of users that {@code userId} is following, with full profile info.
     * Returns an empty list if the user does not exist or has no followings.
     */
    @Async
    public CompletableFuture<List<FollowUserDTO>> getFollowedPage(UUID userId, int page, int size) {
        if (userId == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<FollowId> followed = followRepository.findFollowIdsByUser(
                user, PageRequest.of(page, size)
        ).getContent();

        if (followed.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<UUID> followedIds = followed.stream()
                .map(FollowId::getFollowedId)
                .collect(Collectors.toList());

        List<FollowUserDTO> dtos = userRepository.findAllByIds(followedIds).stream()
                .map(u -> new FollowUserDTO(u.getUuid(), u.getUsername(), u.getProfilePictureUrl()))
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(dtos);
    }

    /**
     * Returns a page of users that follow {@code userId}, with full profile info.
     * Returns an empty list if the user does not exist or has no followers.
     */
    @Async
    public CompletableFuture<List<FollowUserDTO>> getFollowerPage(UUID userId, int page, int size) {
        if (userId == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<FollowId> followers = followRepository.findFollowerIdsByUser(
                user, PageRequest.of(page, size)
        ).getContent();

        if (followers.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<UUID> followerIds = followers.stream()
                .map(FollowId::getUserId)
                .collect(Collectors.toList());

        List<FollowUserDTO> dtos = userRepository.findAllByIds(followerIds).stream()
                .map(u -> new FollowUserDTO(u.getUuid(), u.getUsername(), u.getProfilePictureUrl()))
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(dtos);
    }
}