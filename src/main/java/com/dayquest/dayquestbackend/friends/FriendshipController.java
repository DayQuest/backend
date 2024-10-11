package com.dayquest.dayquestbackend.friends;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    // Send a friend request from one user to another
    @PostMapping("/send-request")
    public ResponseEntity<String> sendFriendRequest(@RequestParam Long userId, @RequestParam String friendUsername) {
        boolean result = friendshipService.sendFriendRequest(userId, friendUsername);
        if (result) {
            return ResponseEntity.ok("Friend request sent");
        } else {
            return ResponseEntity.badRequest().body("Failed to send friend request. The request might already exist or the user was not found.");
        }
    }

    // Accept a friend request
    @PostMapping("/accept-request")
    public ResponseEntity<String> acceptFriendRequest(@RequestParam Long friendshipId) {
        boolean result = friendshipService.acceptFriendRequest(friendshipId);
        if (result) {
            return ResponseEntity.ok("Friend request accepted");
        } else {
            return ResponseEntity.badRequest().body("Failed to accept friend request. The request might already be accepted or it doesn't exist.");
        }
    }

    // List all friends of a user
    @GetMapping("/list")
    public ResponseEntity<List<FriendDTO>> listFriends(@RequestParam Long userId) {
        List<Friendship> friends = friendshipService.getFriends(userId);

        List<FriendDTO> sanitizedFriends = friends.stream()
                .map(friendship -> new FriendDTO(friendship.getFriend().getUsername()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(sanitizedFriends);
    }



    @GetMapping("/pending-requests")
    public ResponseEntity<List<FriendDTO>> getPendingRequests(@RequestParam Long userId) {
        List<Friendship> pendingRequests = friendshipService.getPendingRequests(userId);
        if (pendingRequests != null && !pendingRequests.isEmpty()) {
            List<FriendDTO> sanitizedFriends = pendingRequests.stream()
                    .map(friendship -> new FriendDTO(friendship.getFriend().getUsername()))
                    .toList();
            return ResponseEntity.ok(sanitizedFriends);
        } else {
            return ResponseEntity.ok().body(List.of());
        }
    }
}
