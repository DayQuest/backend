package com.dayquest.dayquestbackend.friends;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    // Send a friend request from one user to another
    @Async
    public CompletableFuture<ResponseEntity<String>> sendFriendRequest(UUID uuid, String friendUsername) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).orElse(null);
            User friend = userRepository.findByUsername(friendUsername);

            if (user != null && friend != null && !user.equals(friend)
                && friendshipRepository.findByUserAndFriend(user, friend) == null
                && friendshipRepository.findByUserAndFriend(friend, user) == null) {

                Friendship friendship = new Friendship();
                friendship.setUser(user);
                friendship.setFriend(friend);
                friendship.setStatus("PENDING");
                friendshipRepository.save(friendship);
                return ResponseEntity.ok("Sent request");
            }
            return ResponseEntity.notFound().build();
        });
    }

    // Accept a friend request
    @Async
    public CompletableFuture<Boolean> acceptFriendRequest(Long friendshipId) {
        return CompletableFuture.supplyAsync(() -> {
            Friendship friendship = friendshipRepository.findById(friendshipId).orElse(null);

            if (friendship != null && "PENDING".equals(friendship.getStatus())) {
                friendship.setStatus("ACCEPTED");
                friendshipRepository.save(friendship);
                return true;
            }
            return false;
        });
    }

    // Get all accepted friends of a user
    @Async
    public CompletableFuture<List<Friendship>> getFriends(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).orElse(null);

            if (user != null) {
                return friendshipRepository.findByUserAndStatus(user, "ACCEPTED");
            }
            return null;
        });
    }

    // Get all pending friend requests for a user
    @Async
    public CompletableFuture<List<Friendship>> getPendingRequests(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).orElse(null);

            if (user != null) {
                return friendshipRepository.findByFriendAndStatus(user, "PENDING");
            }
            return null;
        });
    }
}