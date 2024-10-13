package com.dayquest.dayquestbackend.friends;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
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

  @Autowired
  private FriendshipRepository friendshipRepository;

  @Autowired
  private UserRepository userRepository;

  // Send a friend request from one user to another
  @Async
  @PostMapping("/send-request")
  public CompletableFuture<ResponseEntity<String>> sendFriendRequest(@RequestBody UUID uuid,
      @RequestBody String targetUsername) {
    return friendshipService.sendFriendRequest(uuid, targetUsername);
  }

  // Accept a friend request
  @Async
  @PostMapping("/accept-request")
  public CompletableFuture<ResponseEntity<String>> acceptFriendRequest(
      @RequestBody UUID uuid, @RequestBody UUID targetUuid) {
    return friendshipService.acceptFriendRequest(uuid, targetUuid);
  }

  // List all friends of a user
  @Async
  @GetMapping("/list")
  public CompletableFuture<ResponseEntity<List<FriendDTO>>> listFriends(@RequestParam UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      User user = userRepository.findById(uuid).orElse(null);
      if (user == null) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(friendshipRepository.findByUser(user)
          .stream()
          .map(friendship -> new FriendDTO(friendship.getFriend().getUsername()))
          .toList());
    });
  }

  @Async
  @GetMapping("/pending-requests")
  public CompletableFuture<ResponseEntity<List<FriendDTO>>> getPendingRequests(
      @RequestBody UUID uuid) {
    return friendshipService.getFriendshipsOfUserByState(uuid, FriendRequestStatus.PENDING)
        .thenApply((friendships) -> {
          return ResponseEntity.ok(friendships
              .stream()
              .filter(friendship -> friendship.getStatus() == FriendRequestStatus.PENDING)
              .map(friendship -> new FriendDTO(friendship.getFriend().getUsername()))
              .toList());
        });
  }
}