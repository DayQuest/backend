package com.dayquest.dayquestbackend.friends;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import java.util.Collections;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
                friendship.setStatus(FriendRequestStatus.PENDING);
                friendshipRepository.save(friendship);
                return ResponseEntity.ok("Sent request");
            }
            return ResponseEntity.notFound().build();
        });
    }

    // Accept a friend request
    @Async
    public CompletableFuture<ResponseEntity<String>> acceptFriendRequest(UUID uuid, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).orElse(null);
            User target = userRepository.findById(targetUuid).orElse(null);
            if (user == null || target == null) {
                return ResponseEntity.notFound().build();
            }

            Friendship friendship = friendshipRepository.findByUserAndFriend(user, target);

            if (friendship == null) {
                return ResponseEntity.notFound().build();
            }

            if (friendship.getStatus() != FriendRequestStatus.PENDING) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Friendship ist not pending");
            }

            friendship.setStatus(FriendRequestStatus.ACCEPTED);
            friendshipRepository.save(friendship);
            return ResponseEntity.ok("Accepted friend request");
        });
    }


    // Get all accepted friends of a user
    @Async
    public CompletableFuture<List<Friendship>> getFriendshipsOfUserByState(UUID uuid, FriendRequestStatus state) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).orElse(null);

            if (user == null) {
                return Collections.emptyList();
            }

            return friendshipRepository.findByUserAndStatus(user, state);
        });
    }
}