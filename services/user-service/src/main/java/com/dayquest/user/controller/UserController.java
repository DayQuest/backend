package com.dayquest.user.controller;

import com.dayquest.user.dto.CreateUserDTO;
import com.dayquest.user.dto.ProfileDTO;
import com.dayquest.user.dto.UpdateUserDTO;
import com.dayquest.user.model.User;
import com.dayquest.user.model.UserStatus;
import com.dayquest.user.service.FollowService;
import com.dayquest.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "User Service", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final FollowService followService;

    public UserController(UserService userService, FollowService followService) {
        this.userService = userService;
        this.followService = followService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get user profile by UUID")
    public ResponseEntity<ProfileDTO> getUserById(
            @PathVariable UUID uuid,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        UUID requesterUuid = requesterId != null ? UUID.fromString(requesterId) : null;
        ProfileDTO profile = userService.getProfileById(uuid, requesterUuid);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile/{username}")
    @Operation(summary = "Get user profile by username")
    public ResponseEntity<ProfileDTO> getUserByUsername(
            @PathVariable String username,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        UUID requesterUuid = requesterId != null ? UUID.fromString(requesterId) : null;
        ProfileDTO profile = userService.getProfileByUsername(username, requesterUuid);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{username}/uuid")
    @Operation(summary = "Get user UUID by username")
    public ResponseEntity<UUID> getUuidByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user.getUuid());
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<User> createUser(@RequestBody @Valid CreateUserDTO dto) {
        User user = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID uuid,
            @RequestBody UpdateUserDTO dto) {
        User user = userService.updateUser(uuid, dto);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID uuid) {
        userService.deleteUser(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        UUID requesterUuid = requesterId != null ? UUID.fromString(requesterId) : null;
        Page<ProfileDTO> userPage = userService.searchUsers(query, page, size, requesterUuid);

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/profilepicture/{username}")
    @Operation(summary = "Get user profile picture")
    public ResponseEntity<ByteArrayResource> getProfilePicture(@PathVariable String username) {
        try {
            byte[] imageBytes = userService.getProfilePicture(username);
            
            if (imageBytes == null) {
                ClassPathResource defaultPicture = new ClassPathResource("pfp.jpg");
                imageBytes = defaultPicture.getInputStream().readAllBytes();
            }

            ByteArrayResource resource = new ByteArrayResource(imageBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(imageBytes.length)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{uuid}/profilepicture")
    @Operation(summary = "Update user profile picture")
    public ResponseEntity<String> setProfilePicture(
            @PathVariable UUID uuid,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            userService.updateProfilePicture(uuid, file.getBytes());
            return ResponseEntity.ok("Profile picture uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process the file");
        }
    }

    // Follow endpoints
    @PostMapping("/{uuid}/follow")
    @Operation(summary = "Follow a user")
    public ResponseEntity<String> followUser(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String requesterId) {
        followService.followUser(UUID.fromString(requesterId), uuid);
        return ResponseEntity.ok("User followed");
    }

    @DeleteMapping("/{uuid}/follow")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<String> unfollowUser(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String requesterId) {
        followService.unfollowUser(UUID.fromString(requesterId), uuid);
        return ResponseEntity.ok("User unfollowed");
    }

    @GetMapping("/{uuid}/followers")
    @Operation(summary = "Get user followers")
    public ResponseEntity<Page<UUID>> getFollowers(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UUID> followers = followService.getFollowers(uuid, page, size);
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/{uuid}/following")
    @Operation(summary = "Get users that this user is following")
    public ResponseEntity<Page<UUID>> getFollowing(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UUID> following = followService.getFollowing(uuid, page, size);
        return ResponseEntity.ok(following);
    }

    @GetMapping("/{uuid}/isFollowed")
    @Operation(summary = "Check if current user is following target user")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String requesterId) {
        boolean isFollowing = followService.isFollowing(UUID.fromString(requesterId), uuid);
        return ResponseEntity.ok(isFollowing);
    }

    // Badge endpoints
    @GetMapping("/{uuid}/badges")
    @Operation(summary = "Get user badges")
    public ResponseEntity<List<UUID>> getUserBadges(@PathVariable UUID uuid) {
        List<UUID> badges = userService.getUserBadges(uuid);
        return ResponseEntity.ok(badges);
    }

    @PutMapping("/{uuid}/badge")
    @Operation(summary = "Add badge to user (admin only)")
    public ResponseEntity<String> addBadge(
            @PathVariable UUID uuid,
            @RequestBody UUID badgeId) {
        userService.addBadge(uuid, badgeId);
        return ResponseEntity.ok("Badge added");
    }

    @DeleteMapping("/{uuid}/badge")
    @Operation(summary = "Remove badge from user (admin only)")
    public ResponseEntity<String> removeBadge(
            @PathVariable UUID uuid,
            @RequestBody UUID badgeId) {
        userService.removeBadge(uuid, badgeId);
        return ResponseEntity.ok("Badge removed");
    }

    // Admin endpoints
    @PutMapping("/{uuid}/status")
    @Operation(summary = "Update user status (admin only)")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable UUID uuid,
            @RequestParam UserStatus status,
            @RequestParam(required = false) String adminComment) {
        userService.setBanStatus(uuid, status, adminComment);
        return ResponseEntity.ok("User status updated");
    }

    // Internal endpoints for inter-service communication
    @PostMapping("/internal/enable/{uuid}")
    @Operation(summary = "Enable user account (internal)")
    public ResponseEntity<Void> enableUser(@PathVariable UUID uuid) {
        userService.enableUser(uuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/update-login/{uuid}")
    @Operation(summary = "Update user last login (internal)")
    public ResponseEntity<Void> updateLastLogin(@PathVariable UUID uuid) {
        userService.updateLastLogin(uuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/internal/{uuid}/rerolls")
    @Operation(summary = "Get remaining rerolls (internal)")
    public ResponseEntity<Integer> getRerolls(@PathVariable UUID uuid) {
        int rerolls = userService.getRerolls(uuid);
        return ResponseEntity.ok(rerolls);
    }

    @PostMapping("/internal/{uuid}/daily-quest")
    @Operation(summary = "Set daily quest for user (internal)")
    public ResponseEntity<Void> setDailyQuest(
            @PathVariable UUID uuid,
            @RequestBody UUID questId) {
        userService.setDailyQuest(uuid, questId);
        return ResponseEntity.ok().build();
    }
}
