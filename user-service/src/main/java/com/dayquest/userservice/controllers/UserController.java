package com.dayquest.userservice.controllers;

import com.dayquest.common.dto.UuidDTO;
import com.dayquest.common.events.UserDeletedEvent;
import com.dayquest.common.jwt.JwtService;
import com.dayquest.common.messaging.RabbitMQConstants;
import com.dayquest.userservice.dto.*;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.BadgeRepository;
import com.dayquest.userservice.repositories.UserRepository;
import com.dayquest.userservice.services.FollowService;
import com.dayquest.userservice.services.ProfilePictureService;
import com.dayquest.userservice.services.UserService;
import com.dayquest.userservice.utils.ImageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "Users", description = "User profile management, follows, profile pictures, and badge assignments")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ImageUtil imageUtil;
    private final BadgeRepository badgeRepository;
    private final FollowService followService;
    private final RabbitTemplate rabbitTemplate;
    private final ProfilePictureService profilePictureService;

    public UserController(JwtService jwtService, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                          UserService userService, ImageUtil imageUtil, BadgeRepository badgeRepository,
                          FollowService followService, RabbitTemplate rabbitTemplate,
                          ProfilePictureService profilePictureService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.imageUtil = imageUtil;
        this.badgeRepository = badgeRepository;
        this.followService = followService;
        this.rabbitTemplate = rabbitTemplate;
        this.profilePictureService = profilePictureService;
    }

    @DeleteMapping({"", "/"})
    @Async
    @Operation(summary = "Delete account", description = "Permanently deletes the authenticated user's account. Requires current password confirmation. Publishes a UserDeletedEvent to remove data across services.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public CompletableFuture<ResponseEntity<?>> deleteUser(@RequestHeader("Authorization") String token, @RequestBody PasswordDTO passwordDTO) {
        String tokenWithoutBearer = token.substring(7);
        UUID userId = jwtService.extractUserId(tokenWithoutBearer);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !passwordEncoder.matches(passwordDTO.getPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials"));
        }

        String username = user.getUsername();
        UserDeletedEvent event = new UserDeletedEvent(userId, username);
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConstants.USER_EXCHANGE,
                RabbitMQConstants.USER_DELETED_ROUTING_KEY,
                event
            );
            logger.info("Published UserDeletedEvent for user: {} ({})", username, userId);
        } catch (Exception e) {
            logger.error("Failed to publish UserDeletedEvent for user: {} - {}", userId, e.getMessage());
        }

        userRepository.delete(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("User deleted successfully"));
    }

    @GetMapping("/{uuid}")
    @Async
    @Operation(summary = "Get user by UUID", description = "Returns the full profile of the user with the given UUID, including follow status relative to the requester.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUuid(
            @Parameter(description = "UUID of the user to retrieve", required = true) @PathVariable UUID uuid,
            @RequestHeader("Authorization") String token) {
        Optional<User> userWithVideos = userRepository.findById(uuid);
        if (userWithVideos.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        }
        UUID requesterUUID = jwtService.extractUserId(token.substring(7));
        User requester = userRepository.findById(requesterUUID).orElse(null);
        ProfileDTO profileDTO = userService.createProfileDTO(userWithVideos.get(), requester).join();
        return CompletableFuture.completedFuture(ResponseEntity.ok(profileDTO));
    }

    @GetMapping("/profile/{username}")
    @Async
    @Transactional(readOnly = true)
    @Operation(summary = "Get user profile by username", description = "Returns the full profile of the user with the given username.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUsername(
            @Parameter(description = "Username to look up", required = true) @PathVariable String username,
            @RequestHeader("Authorization") String token) {
            User userWithVideos = userRepository.findByUsername(username);
            Optional<User> user = userRepository.findById(jwtService.extractUserId(token.substring(7)));
            if (userWithVideos == null) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }
            return CompletableFuture.completedFuture(ResponseEntity.ok(userService.createProfileDTO(userWithVideos, user.get()).join()));
    }

    @GetMapping("/search")
    @Async
    @Operation(summary = "Search users by username", description = "Searches for users whose username contains the given query string. Supports pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "404", description = "No users found")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> searchUsers(
            @Parameter(description = "Search query (1–100 characters)", required = true)
            @RequestParam @Size(min = 1, max = 100, message = "Query must be between 1 and 100 characters") String query,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1–100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
            Page<User> userPage = userRepository.findUsersByUsernameContainingIgnoreCase(query, PageRequest.of(page, size));
            List<ProfileDTO> profileDTOs = userPage.getContent().stream()
                    .map(user -> userService.createProfileDTO(user, null).join())
                    .collect(Collectors.toList());

            if (profileDTOs.isEmpty()) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", profileDTOs);
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));
    }

    @GetMapping("/{username}/uuid")
    @Async
    @Operation(summary = "Get UUID by username", description = "Returns the UUID of the user with the given username. Useful for service-to-service lookups.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "UUID returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public CompletableFuture<ResponseEntity<UUID>> getUuidByUsername(
            @Parameter(description = "Username to resolve", required = true) @PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }
        return CompletableFuture.completedFuture(ResponseEntity.ok(user.getUuid()));
    }

    @GetMapping("/profilepicture/{username}")
    @Async
    @Operation(summary = "Get profile picture", description = "Returns the profile picture of the user as a JPEG image. Falls back to a default image if none is set. This endpoint is public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile picture returned (image/jpeg)"),
            @ApiResponse(responseCode = "500", description = "Error retrieving image")
    })
    public CompletableFuture<ResponseEntity<ByteArrayResource>> getDecodedImage(
            @Parameter(description = "Username of the profile picture owner", required = true) @PathVariable("username") String username) {
        try {
            User user = userRepository.findByUsername(username);
            byte[] imageBytes = null;

            if (user != null && profilePictureService.isMinioAvailable()) {
                imageBytes = profilePictureService.getProfilePicture(user.getUuid());
            }


            // Default picture if nothing found
            if (imageBytes == null) {
                ClassPathResource defaultPicture = new ClassPathResource("pfp.jpg");
                imageBytes = defaultPicture.getInputStream().readAllBytes();
            }

            ByteArrayResource resource = new ByteArrayResource(imageBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("public, max-age=86400"); // Cache for 1 day

            return CompletableFuture.completedFuture(ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(imageBytes.length)
                    .body(resource));

        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error retrieving profile picture for {}: {}", username, e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ByteArrayResource(new byte[0])));
        }
    }
    @PutMapping("/email")
    @Async
    @Operation(summary = "Update email address", description = "Changes the authenticated user's email address. Requires current password confirmation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email updated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid password"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public CompletableFuture<ResponseEntity<?>> updateEmail (@RequestBody @Valid UpdateEmailDTO updateEmailDTO,
                                                             @RequestHeader("Authorization") String token){
        Optional<User> userOptional = userRepository.findById(jwtService.extractUserId(token.substring(7)));
        if (userOptional.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password"));
        }
        if (userRepository.findByEmailIgnoreCase(updateEmailDTO.getEmail()).isPresent()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT).body("Email already taken"));
        }
        user.setEmail(updateEmailDTO.getEmail());
        userRepository.save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Email updated successfully"));
    }

    @PutMapping("/password")
    @Async
    @Operation(summary = "Update password", description = "Changes the authenticated user's password. Requires the current password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid old password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public CompletableFuture<ResponseEntity<String>> updatePassword(@RequestBody @Valid UpdatePasswordDTO updatePasswordDTO,
                                                                    @RequestHeader("Authorization") String token) {
        Optional<User> userOptional = userRepository.findById(jwtService.extractUserId(token.substring(7)));
        if (userOptional.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid old password"));
        }
        user.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        userRepository.save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Password updated successfully"));
    }

    @PostMapping("/setprofilepicture")
    @Async
    @Operation(summary = "Upload profile picture", description = "Uploads and sets a new profile picture for the authenticated user. The image is compressed and stored in MinIO. Supported formats: JPEG, PNG.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "File is empty"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    public CompletableFuture<ResponseEntity<String>> setProfilePicture(
            @Parameter(description = "Image file to upload (JPEG/PNG)", required = true) @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("File is empty"));
        }

        try {
            Optional<User> userOptional = userRepository.findById(jwtService.extractUserId(token.substring(7)));
            if(userOptional.isEmpty()){
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
            }
            User user = userOptional.get();
            byte[] fileBytes = imageUtil.compressImage(file).join();

            // Try MinIO first, fallback to database
            if (profilePictureService.isMinioAvailable()) {
                String url = profilePictureService.uploadProfilePicture(user.getUuid(), fileBytes).join();
                if (url != null) {
                    user.setProfilePictureUrl(url);
                    userRepository.save(user);
                    logger.info("Profile picture uploaded to MinIO for user: {}", user.getUsername());
                    return CompletableFuture.completedFuture(ResponseEntity.ok("Profile picture uploaded successfully"));
                }
            }
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Please try again later"));
        } catch (IOException e) {
            logger.error("Failed to process profile picture: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process the file"));
        }
    }

    @GetMapping("/rerolls")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getRerolls(@RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
            }
            User user = userOptional.get();
            return user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now()) 
                    ? ResponseEntity.ok(3) : ResponseEntity.ok(user.getLeftRerolls());
        });
    }

    //TODO: Move to admin microservice
    //TODO: Test this endpoint and make sure it works correctly with the badge system. 
    // Also consider edge cases like adding a badge that doesn't exist or adding a badge to a user that doesn't exist.
    @PutMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> addBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId, 
                                                              @RequestHeader("Authorization") String token) {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
            }
            User user = userOptional.get();
            if (user.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not an admin"));
            }
            User userToAddBadge = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
            if (userToAddBadge.getBadges().contains(badgeId)) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT).body("Badge already added"));
            }
            userToAddBadge.getBadges().add(badgeId);
            userRepository.save(userToAddBadge);
            return CompletableFuture.completedFuture(ResponseEntity.ok("Badge added"));
    }

    //TODO: refactor the return type of this endpoint to return more information about the badges instead of just the ids. 
    // Also consider edge cases like requesting badges for a user that doesn't exist.
    @GetMapping("/{uuid}/badges")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getBadges(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(badgeRepository.findBadgeIdsByUserId(uuid)));
    }


    //TODO: Add more Information like pfp url username etc. 
    // Also consider edge cases like requesting followers for a user that doesn't exist or requesting a page that is out of bounds.
    @GetMapping("/{uuid}/followers")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowers(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, 
                                                                      @RequestParam(defaultValue = "10") int size, 
                                                                      @RequestHeader("Authorization") String token) {
            CompletableFuture<List<UUID>> followersFuture = followService.getFollowerPage(uuid, page, size);
            List<UUID> followers = followersFuture.join();
            return CompletableFuture.completedFuture(ResponseEntity.ok(followers));
    }

    //TODO: Add more Information like pfp url username etc. 
    // Also consider edge cases like requesting followers for a user that doesn't exist or requesting a page that is out of bounds.
    @GetMapping("/{uuid}/following")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowing(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, 
                                                                      @RequestParam(defaultValue = "10") int size, 
                                                                      @RequestHeader("Authorization") String token) {
            CompletableFuture<List<UUID>> followingFuture = followService.getFollowedPage(uuid, page, size);
            List<UUID> following = followingFuture.join();
            return CompletableFuture.completedFuture(ResponseEntity.ok(following));
    }

    @PostMapping("/{uuid}/follow")
    @Async
    @Operation(summary = "Follow a user", description = "Follows the user with the given UUID. Cannot follow yourself or a user you already follow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User followed"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Already following this user"),
            @ApiResponse(responseCode = "422", description = "Cannot follow yourself")
    })
    public CompletableFuture<ResponseEntity<String>> followUser(
            @Parameter(description = "UUID of the user to follow", required = true) @PathVariable UUID uuid,
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) UuidDTO videoUuid) {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            User userToFollow = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
            if (userOptional.isEmpty()) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
            }
            User user = userOptional.get();
            if (uuid.equals(user.getUuid())) {
                return CompletableFuture.completedFuture(ResponseEntity.unprocessableEntity().body("Cannot follow yourself"));
            }
            if (followService.isFollowing(user.getUuid(), uuid).join()) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT).body("User already followed"));
            }

            userToFollow.setFollowers(userToFollow.getFollowers() + 1);
            followService.followUser(token.substring(7), uuid).join();
            userRepository.save(userToFollow);

            return CompletableFuture.completedFuture(ResponseEntity.ok("User followed"));
    }

    @DeleteMapping("/{uuid}/follow")
    @Async
    @Operation(summary = "Unfollow a user", description = "Unfollows the user with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User unfollowed"),
            @ApiResponse(responseCode = "404", description = "User not found or not following")
    })
    public CompletableFuture<ResponseEntity<String>> unfollowUser(
            @Parameter(description = "UUID of the user to unfollow", required = true) @PathVariable UUID uuid,
            @RequestHeader("Authorization") String token) {
        return followService.unfollowUser(token.substring(7), uuid)
                .thenApply(response -> {
                    if (response.getStatusCode() == HttpStatus.OK) {
                        User userToUnfollow = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
                        userToUnfollow.setFollowers(userToUnfollow.getFollowers() - 1);
                        userRepository.save(userToUnfollow);
                    }
                    return response;
                });
    }

    @GetMapping("{username}/followersAsInt")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getFollowersAsInt(@PathVariable String username) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }
            return CompletableFuture.completedFuture(ResponseEntity.ok(user.getFollowers()));
    }

    @GetMapping("{uuid}/isFollowed")
    @Async
    public CompletableFuture<ResponseEntity<Boolean>> isFollowed(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(false));
        }
        UUID username = jwtService.extractUserId(token.substring(7));
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> followService.isFollowing(value.getUuid(), uuid).join()
                ? CompletableFuture.completedFuture(ResponseEntity.ok(true))
                : CompletableFuture.completedFuture(ResponseEntity.ok(false)))
                .orElseGet(() -> CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body(false)));
    }

    //TODO: Move to admin microservice
    @DeleteMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> removeBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId, 
                                                                 @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            User user = userOptional.get();
            if (user.getAuthorities().stream()
                    .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not an admin");
            }
            User userToRemoveBadge = userRepository.findById(uuid).orElse(null);
            if (userToRemoveBadge == null) {
                return ResponseEntity.notFound().build();
            }
            if (!userToRemoveBadge.getBadges().contains(badgeId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Badge not found");
            }
            userToRemoveBadge.getBadges().remove(badgeId);
            userRepository.save(userToRemoveBadge);
            return ResponseEntity.ok("Badge removed");
        });
    }

}
