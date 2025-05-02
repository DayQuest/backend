package com.dayquest.dayquestbackend.user.services;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.user.ids.FollowId;
import com.dayquest.dayquestbackend.user.models.Follow;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.repositories.FollowRepository;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class FollowService {
    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private JwtService jwtService;
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

    @Async
    public CompletableFuture<List<UUID>> getFollowedPage(String token, int page, int size) {
        UUID userId = jwtService.extractUserId(token);
        if (userId == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<FollowId> followed = followRepository.findFollowIdsByUser(userRepository.findById(userId).orElse(null), PageRequest.of(page, size)).getContent();
        if (followed.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<UUID> followedIds = followed.stream()
                .map(FollowId::getFollowedId)
                .toList();
        return CompletableFuture.completedFuture(followedIds);
    }

    @Async
    public CompletableFuture<List<UUID>> getFollowerPage(String token, int page, int size) {
        UUID userId = jwtService.extractUserId(token);
        if (userId == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<FollowId> followers = followRepository.findFollowerIdsByUser(userRepository.findById(userId).orElse(null), PageRequest.of(page, size)).getContent();
        if (followers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<UUID> followerIds = followers.stream()
                .map(FollowId::getUserId)
                .toList();
        return CompletableFuture.completedFuture(followerIds);
    }
}
