package com.example.dayquest.service;

import com.example.dayquest.friends.Friendship;
import com.example.dayquest.user.User;
import com.example.dayquest.friends.FriendshipRepository;
import com.example.dayquest.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    // Send a friend request from one user to another
    public boolean sendFriendRequest(Long userId, String friendUsername) {
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
    }

    // Accept a friend request
    public boolean acceptFriendRequest(Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId).orElse(null);

        if (friendship != null && "PENDING".equals(friendship.getStatus())) {
            friendship.setStatus("ACCEPTED");
            friendshipRepository.save(friendship);
            return true;
        }
        return false;
    }

    // Get all accepted friends of a user
    public List<Friendship> getFriends(Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user != null) {
            return friendshipRepository.findByUserAndStatus(user, "ACCEPTED");
        }
        return null;
    }

    // Get all pending friend requests for a user
    public List<Friendship> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user != null) {
            return friendshipRepository.findByFriendAndStatus(user, "PENDING");
        }
        return null;
    }
}
