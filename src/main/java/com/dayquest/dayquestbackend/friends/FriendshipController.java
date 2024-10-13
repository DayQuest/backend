package com.dayquest.dayquestbackend.friends;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

  @Autowired
  private FriendshipService friendshipService;

  // Send a friend request from one user to another
  @Async
  @PostMapping("/send-request")
  public CompletableFuture<ResponseEntity<String>> sendFriendRequest(@RequestParam UUID uuid,
      @RequestParam String targetUsername) {
    return friendshipService.sendFriendRequest(uuid, targetUsername);
  }

  // Accept a friend request
  @Async
  @PostMapping("/accept-request")
  public CompletableFuture<ResponseEntity<String>> acceptFriendRequest(
      @RequestParam UUID user) {
    return friendshipService.acceptFriendRequest(friendshipId)
        .thenApply(result -> {
          if (result) {
            return ResponseEntity.ok("Friend request accepted");
          } else {
            return ResponseEntity.badRequest().body(
                "Failed to accept friend request. The request might already be accepted or it doesn't exist.");
          }
        });
  }

  // List all friends of a user
  @Async
  @GetMapping("/list")
  public CompletableFuture<ResponseEntity<List<FriendDTO>>> listFriends(@RequestParam UUID uuid) {
    return friendshipService.getFriends(userId)
        .thenApply(friends -> {
          if (friends != null) {
            List<FriendDTO> sanitizedFriends = friends.stream()
                .map(friendship -> new FriendDTO(friendship.getFriend().getUsername()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(sanitizedFriends);
          } else {
            return ResponseEntity.ok(List.of());
          }
        });
  }

  @Async
  @GetMapping("/pending-requests")
  public CompletableFuture<ResponseEntity<List<UUID>>> getPendingRequests(
      @RequestParam UUID uuid) {
    return friendshipService.getFriendshipsOfUserByState(uuid, FriendRequestStatus.PENDING);
  }
}