package com.dayquest.dayquestbackend.friends;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    public CompletableFuture<Boolean> sendFriendRequest(Long userId, String friendUsername) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId).orElse(null);
            User friend = userRepository.findByUsername(friendUsername);

            if (user != null && friend != null && !user.equals(friend)
                && friendshipRepository.findByUserAndFriend(user, friend) == null
                && friendshipRepository.findByUserAndFriend(friend, user) == null) {

                Friendship friendship = new Friendship();
                friendship.setUser(user);
                friendship.setFriend(friend);
                friendship.setStatus("PENDING");
                friendshipRepository.save(friendship);
                return true;
            }
            return false;
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
    public CompletableFuture<List<Friendship>> getFriends(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                return friendshipRepository.findByUserAndStatus(user, "ACCEPTED");
            }
            return null;
        });
    }

    // Get all pending friend requests for a user
    @Async
    public CompletableFuture<List<Friendship>> getPendingRequests(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                return friendshipRepository.findByFriendAndStatus(user, "PENDING");
            }
            return null;
        });
    }
}