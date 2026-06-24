package com.dayquest.socialservice.controller;

import com.dayquest.socialservice.dto.FriendshipDTO;
import com.dayquest.socialservice.model.Friendship;
import com.dayquest.socialservice.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;


//TODO: Fully rethink/rewrite the friendship system and include to other microservices for only friend group quests/videos etc
@RestController
@RequestMapping("/friends")
@Tag(name = "Friendships", description = "Friendship management endpoints")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/request/{friendUuid}")
    @Operation(summary = "Send a friend request")
    public ResponseEntity<FriendshipDTO> sendFriendRequest(
            @PathVariable UUID friendUuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        if (userUuid.equals(friendUuid)) {
            return ResponseEntity.badRequest().build();
        }

        Friendship friendship = friendshipService.sendFriendRequest(userUuid, friendUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendshipService.toDTO(friendship, userUuid));
    }

    @PostMapping("/{friendshipId}/accept")
    @Operation(summary = "Accept a friend request")
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable UUID friendshipId,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        boolean accepted = friendshipService.acceptFriendRequest(friendshipId, userUuid);
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot accept friend request"));
        }

        return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
    }

    @PostMapping("/{friendshipId}/decline")
    @Operation(summary = "Decline a friend request")
    public ResponseEntity<?> declineFriendRequest(
            @PathVariable UUID friendshipId,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        boolean declined = friendshipService.declineFriendRequest(friendshipId, userUuid);
        if (!declined) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot decline friend request"));
        }

        return ResponseEntity.ok(Map.of("message", "Friend request declined"));
    }

    @DeleteMapping("/{friendUuid}")
    @Operation(summary = "Remove a friend")
    public ResponseEntity<?> removeFriend(
            @PathVariable UUID friendUuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        boolean removed = friendshipService.removeFriend(userUuid, friendUuid);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Friend removed"));
    }

    @PostMapping("/block/{blockedUuid}")
    @Operation(summary = "Block a user")
    public ResponseEntity<?> blockUser(
            @PathVariable UUID blockedUuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);
        friendshipService.blockUser(userUuid, blockedUuid);
        return ResponseEntity.ok(Map.of("message", "User blocked"));
    }

    @GetMapping
    @Operation(summary = "Get all friends")
    public ResponseEntity<Page<FriendshipDTO>> getFriends(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        Page<Friendship> friends = friendshipService.getFriends(userUuid, PageRequest.of(page, size));
        Page<FriendshipDTO> dtos = friends.map(f -> friendshipService.toDTO(f, userUuid));

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending friend requests")
    public ResponseEntity<Page<FriendshipDTO>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        Page<Friendship> requests = friendshipService.getPendingRequests(userUuid, PageRequest.of(page, size));
        Page<FriendshipDTO> dtos = requests.map(f -> friendshipService.toDTO(f, userUuid));

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/status/{otherUserUuid}")
    @Operation(summary = "Get friendship status with another user")
    public ResponseEntity<?> getFriendshipStatus(
            @PathVariable UUID otherUserUuid,
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);

        return friendshipService.getFriendshipStatus(userUuid, otherUserUuid)
                .<ResponseEntity<?>>map(f -> ResponseEntity.ok(friendshipService.toDTO(f, userUuid)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("status", "NONE")));
    }

    @GetMapping("/count")
    @Operation(summary = "Get friend count")
    public ResponseEntity<Map<String, Long>> getFriendCount(
            @RequestHeader("X-User-Id") String userIdHeader) {

        UUID userUuid = UUID.fromString(userIdHeader);
        long count = friendshipService.getFriendCount(userUuid);

        return ResponseEntity.ok(Map.of("count", count));
    }
}

